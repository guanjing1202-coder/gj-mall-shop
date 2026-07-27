package com.gj.mall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.cart.service.CartService;
import com.gj.mall.cart.vo.CartItemVO;
import com.gj.mall.cart.vo.CartVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.CreateOrderDTO;
import com.gj.mall.order.dto.CreateOrderItemDTO;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.dto.InvoiceInfoDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mq.OrderTimeoutProducer;
import com.gj.mall.order.service.FreightService;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.order.vo.FreightQuoteVO;
import com.gj.mall.order.vo.OrderItemVO;
import com.gj.mall.order.vo.InvoiceVO;
import com.gj.mall.order.vo.OrderLogisticsTraceVO;
import com.gj.mall.order.vo.OrderLogisticsVO;
import com.gj.mall.order.vo.OrderVO;
import com.gj.mall.order.vo.ReceiverVO;
import com.gj.mall.order.vo.AdminOrderFulfillmentSummaryVO;
import com.gj.mall.marketing.entity.SmsSeckillSku;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.service.impl.SeckillServiceImpl;
import com.gj.mall.marketing.vo.CouponCheckResult;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.service.UserAddressService;
import com.gj.mall.user.service.UserMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final CartService cartService;
    private final SkuService skuService;
    private final UserAddressService addressService;
    private final OrderTimeoutProducer timeoutProducer;
    private final CouponService couponService;
    private final PmsSpuMapper spuMapper;
    private final UserMessageService messageService;
    private final FreightService freightService;
    private final SeckillServiceImpl seckillService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String create(Long userId, CreateOrderDTO dto) {
        boolean directBuy = CollUtil.isNotEmpty(dto.getItems());
        // 1. 取购物车选中项，或按直购商品生成结算行
        List<CartItemVO> selected = directBuy
                ? buildDirectOrderItems(dto.getItems())
                : buildCartOrderItems(userId);
        if (selected.isEmpty()) {
            throw new BizException(ResultCode.ORDER_EMPTY_ITEMS, "请先勾选有效商品");
        }

        // 2. 收货地址快照
        UmsUserAddress addr = addressService.getOne(userId, dto.getAddressId());
        ReceiverVO receiver = new ReceiverVO();
        BeanUtil.copyProperties(addr, receiver);

        // 3. 预扣库存（行级原子 update）
        Map<Long, Integer> lockedQtyMap = new LinkedHashMap<>();
        for (CartItemVO item : selected) {
            boolean ok = skuService.lockStock(item.getSkuId(), item.getQuantity());
            if (!ok) {
                // 回滚已扣的
                lockedQtyMap.forEach(skuService::releaseStock);
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH,
                        "SKU=" + item.getSkuId() + " 库存不足");
            }
            lockedQtyMap.merge(item.getSkuId(), item.getQuantity(), Integer::sum);
        }

        // 4. 计算金额 + 优惠券
        BigDecimal totalAmount = selected.stream()
                .map(i -> i.getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        FreightQuoteVO freightQuote = freightService.calculate(totalAmount, addr);
        BigDecimal freight = freightQuote.getFreightAmount();
        CouponCheckResult couponResult = couponService.check(userId, dto.getCouponId(), totalAmount);
        BigDecimal coupon  = couponResult.getDiscountAmount();
        BigDecimal payAmount = totalAmount.add(freight).subtract(coupon);
        if (payAmount.signum() < 0) payAmount = BigDecimal.ZERO;

        // 5. 落库订单
        OmsOrder order = new OmsOrder();
        String orderNo = genOrderNo(userId);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setFreightAmount(freight);
        order.setCouponAmount(coupon);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setReceiverInfo(JSON.toJSONString(receiver));
        InvoiceVO invoice = normalizeInvoice(dto.getInvoiceInfo());
        if (invoice != null) {
            order.setInvoiceInfo(JSON.toJSONString(invoice));
        }
        order.setRemark(dto.getRemark());
        order.setCouponUserId(couponResult.getCouponUserId());  // 可为 null
        orderMapper.insert(order);

        // 6. 落库订单项
        for (CartItemVO item : selected) {
            OmsOrderItem oi = new OmsOrderItem();
            oi.setOrderId(order.getId());
            oi.setOrderNo(orderNo);
            oi.setSpuId(item.getSpuId());
            oi.setSkuId(item.getSkuId());
            oi.setSkuName(item.getSkuName());
            oi.setSkuImage(item.getImage());
            if (item.getSpecData() != null && !item.getSpecData().isEmpty()) {
                oi.setSpecData(JSON.toJSONString(item.getSpecData()));
            }
            oi.setPrice(item.getPrice());
            oi.setQuantity(item.getQuantity());
            oi.setTotalAmount(item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            itemMapper.insert(oi);
        }

        // 7. 购物车下单后移除购物车里的选中项，直购不影响购物车
        if (!directBuy) {
            for (CartItemVO item : selected) {
                try { cartService.remove(userId, item.getSkuId()); } catch (Exception ignored) {}
            }
        }

        // 8. 发送延迟超时检查
        try {
            timeoutProducer.send(order.getId());
        } catch (Exception e) {
            log.warn("[order] send timeout MQ failed, orderId={}", order.getId(), e);
        }

        log.info("[order] created orderNo={} userId={} payAmount={}", orderNo, userId, payAmount);
        notifyUser(order.getUserId(), "order", "订单已提交", "订单 " + order.getOrderNo() + " 已提交，请及时完成支付。", "order", order.getId(), order.getOrderNo());
        return orderNo;
    }

    private List<CartItemVO> buildCartOrderItems(Long userId) {
        CartVO cart = cartService.get(userId);
        if (cart == null || CollUtil.isEmpty(cart.getItems())) {
            throw new BizException(ResultCode.ORDER_EMPTY_ITEMS);
        }
        return cart.getItems().stream()
                .filter(i -> Integer.valueOf(1).equals(i.getSelected()))
                .filter(i -> !Boolean.TRUE.equals(i.getInvalid()))
                .filter(i -> Boolean.TRUE.equals(i.getStockEnough()))
                .collect(Collectors.toList());
    }

    private List<CartItemVO> buildDirectOrderItems(List<CreateOrderItemDTO> items) {
        if (CollUtil.isEmpty(items)) {
            throw new BizException(ResultCode.ORDER_EMPTY_ITEMS);
        }
        List<CreateOrderItemDTO> normalized = items.stream()
                .filter(Objects::nonNull)
                .filter(item -> item.getSkuId() != null)
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(normalized)) {
            throw new BizException(ResultCode.ORDER_EMPTY_ITEMS);
        }
        Map<Long, Integer> qtyBySku = new LinkedHashMap<>();
        for (CreateOrderItemDTO item : normalized) {
            int quantity = item.getQuantity() == null ? 1 : item.getQuantity();
            if (quantity <= 0) {
                throw new BizException(ResultCode.PARAM_ERROR, "购买数量必须大于 0");
            }
            qtyBySku.merge(item.getSkuId(), quantity, Integer::sum);
        }
        return qtyBySku.entrySet().stream()
                .map(e -> toDirectCartItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList());
    }

    private CartItemVO toDirectCartItem(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "购买数量必须大于 0");
        }
        PmsSku sku = skuService.getByIdOrThrow(skuId);
        PmsSpu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null) {
            throw new BizException(ResultCode.PRODUCT_NOT_FOUND);
        }
        if (!Integer.valueOf(1).equals(spu.getPublishStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }
        if (sku.getStock() == null || sku.getStock() < quantity) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }
        CartItemVO line = new CartItemVO();
        line.setSkuId(sku.getId());
        line.setSpuId(spu.getId());
        line.setSpuName(spu.getName());
        line.setSkuName(sku.getName());
        line.setImage(StrUtil.isBlank(sku.getImage()) ? spu.getMainImage() : sku.getImage());
        line.setPrice(sku.getPrice());
        line.setStock(sku.getStock());
        line.setPublishStatus(spu.getPublishStatus());
        line.setInvalid(false);
        line.setSpecData(parseSpecData(sku.getSpecData()));
        line.setQuantity(quantity);
        line.setSelected(1);
        line.setTotalAmount(sku.getPrice().multiply(BigDecimal.valueOf(quantity)));
        return line;
    }

    private Map<String, String> parseSpecData(String specData) {
        if (StrUtil.isBlank(specData)) {
            return Collections.emptyMap();
        }
        try {
            return JSON.parseObject(specData, new TypeReference<Map<String, String>>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private InvoiceVO normalizeInvoice(InvoiceInfoDTO dto) {
        if (dto == null || dto.getType() == null || Integer.valueOf(0).equals(dto.getType())) {
            return null;
        }
        if (!Integer.valueOf(1).equals(dto.getType()) && !Integer.valueOf(2).equals(dto.getType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "发票类型非法");
        }
        String title = StrUtil.trim(dto.getTitle());
        if (StrUtil.isBlank(title)) {
            throw new BizException(ResultCode.PARAM_MISSING, "请填写发票抬头");
        }
        String taxNo = StrUtil.trim(dto.getTaxNo());
        if (Integer.valueOf(2).equals(dto.getType()) && StrUtil.isBlank(taxNo)) {
            throw new BizException(ResultCode.PARAM_MISSING, "企业发票请填写纳税人识别号");
        }
        InvoiceVO vo = new InvoiceVO();
        vo.setType(dto.getType());
        vo.setTitle(StrUtil.sub(title, 0, 100));
        vo.setTaxNo(StrUtil.isBlank(taxNo) ? null : StrUtil.sub(taxNo, 0, 32));
        vo.setEmail(StrUtil.isBlank(dto.getEmail()) ? null : StrUtil.sub(StrUtil.trim(dto.getEmail()), 0, 100));
        vo.setContent(StrUtil.sub(StrUtil.blankToDefault(StrUtil.trim(dto.getContent()), "商品明细"), 0, 50));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long orderId) {
        OmsOrder order = mustOwn(userId, orderId);
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "仅待付款订单可取消");
        }
        doCancel(order);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void timeoutCancel(Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null) return;
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            // 已支付或已取消，跳过
            return;
        }
        log.info("[order] timeout cancel orderId={}", orderId);
        doCancel(order);
    }

    private void doCancel(OmsOrder order) {
        // 释放商品库存
        List<OmsOrderItem> items = itemMapper.selectList(
                Wrappers.<OmsOrderItem>lambdaQuery().eq(OmsOrderItem::getOrderId, order.getId()));
        for (OmsOrderItem it : items) {
            skuService.releaseStock(it.getSkuId(), it.getQuantity());
        }
        // 释放优惠券
        if (order.getCouponUserId() != null) {
            couponService.release(order.getCouponUserId());
        }
        OmsOrder upd = new OmsOrder();
        upd.setId(order.getId());
        upd.setStatus(OrderStatus.CANCELED.getCode());
        orderMapper.updateById(upd);
        notifyUser(order.getUserId(), "order", "订单已取消", "订单 " + order.getOrderNo() + " 已取消，锁定库存和优惠券已释放。", "order", order.getId(), order.getOrderNo());
    }

    @Override
    public PageResult<OrderVO> page(Long userId, OrderQueryDTO q) {
        Page<OmsOrder> page = new Page<>(safe(q.getPageNum(), 1L), safe(q.getPageSize(), 10L));
        Page<OmsOrder> result = orderMapper.selectPage(page,
                Wrappers.<OmsOrder>lambdaQuery()
                        .eq(OmsOrder::getUserId, userId)
                        .eq(q.getStatus() != null, OmsOrder::getStatus, q.getStatus())
                        .orderByDesc(OmsOrder::getId));
        return toPageVO(result, true);
    }

    @Override
    public PageResult<OrderVO> adminPage(OrderQueryDTO q) {
        Page<OmsOrder> page = new Page<>(safe(q.getPageNum(), 1L), safe(q.getPageSize(), 10L));
        Page<OmsOrder> result = orderMapper.selectPage(page,
                Wrappers.<OmsOrder>lambdaQuery()
                        .eq(q.getUserId() != null, OmsOrder::getUserId, q.getUserId())
                        .like(StrUtil.isNotBlank(q.getOrderNo()), OmsOrder::getOrderNo, q.getOrderNo())
                        .like(StrUtil.isNotBlank(q.getDeliveryNo()), OmsOrder::getDeliveryNo, q.getDeliveryNo())
                        .eq(q.getStatus() != null, OmsOrder::getStatus, q.getStatus())
                        .orderByDesc(OmsOrder::getId));
        return toPageVO(result, true);
    }

    @Override
    public AdminOrderFulfillmentSummaryVO adminFulfillmentSummary() {
        AdminOrderFulfillmentSummaryVO summary = orderMapper.selectFulfillmentSummary();
        if (summary == null) {
            return new AdminOrderFulfillmentSummaryVO();
        }
        return summary;
    }

    private long safe(Long v, long fallback) {
        return v == null || v <= 0 ? fallback : v;
    }

    private PageResult<OrderVO> toPageVO(Page<OmsOrder> page, boolean withItems) {
        if (CollUtil.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }
        List<Long> orderIds = page.getRecords().stream().map(OmsOrder::getId).collect(Collectors.toList());
        Map<Long, List<OrderItemVO>> grouped = new HashMap<>();
        if (withItems) {
            List<OmsOrderItem> all = itemMapper.selectList(
                    Wrappers.<OmsOrderItem>lambdaQuery().in(OmsOrderItem::getOrderId, orderIds));
            for (OmsOrderItem it : all) {
                grouped.computeIfAbsent(it.getOrderId(), k -> new ArrayList<>()).add(toItemVO(it));
            }
        }
        List<OrderVO> list = page.getRecords().stream()
                .map(o -> toOrderVO(o, grouped.getOrDefault(o.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), list);
    }

    @Override
    public OrderVO detail(Long userId, Long orderId) {
        OmsOrder order = mustOwn(userId, orderId);
        return detailVO(order);
    }

    @Override
    public OrderVO detailByOrderNo(Long userId, String orderNo) {
        if (StrUtil.isBlank(orderNo)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        OmsOrder order = orderMapper.selectOne(Wrappers.<OmsOrder>lambdaQuery()
                .eq(OmsOrder::getUserId, userId)
                .eq(OmsOrder::getOrderNo, orderNo)
                .last("limit 1"));
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return detailVO(order);
    }

    @Override
    public OrderLogisticsVO logistics(Long userId, Long orderId) {
        OmsOrder order = mustOwn(userId, orderId);
        OrderStatus status = OrderStatus.of(order.getStatus());
        OrderLogisticsVO vo = new OrderLogisticsVO();
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setStatus(order.getStatus());
        vo.setStatusDesc(status == null ? "未知" : status.getDesc());
        vo.setDeliveryCompany(order.getDeliveryCompany());
        vo.setDeliveryNo(order.getDeliveryNo());
        vo.setDeliveryRemark(order.getDeliveryRemark());
        vo.setCurrentAction(logisticsCurrentAction(order));
        vo.setNextHint(logisticsNextHint(order));
        vo.setTraces(buildLogisticsTraces(order));
        return vo;
    }

    private List<OrderLogisticsTraceVO> buildLogisticsTraces(OmsOrder order) {
        Integer status = order.getStatus();
        boolean canceled = OrderStatus.CANCELED.getCode().equals(status);
        boolean refunded = OrderStatus.REFUNDED.getCode().equals(status);
        boolean refunding = OrderStatus.REFUNDING.getCode().equals(status);
        List<OrderLogisticsTraceVO> traces = new ArrayList<>();
        traces.add(new OrderLogisticsTraceVO(
                "订单提交",
                "订单已创建，等待买家完成支付",
                order.getCreateTime(),
                true));
        traces.add(new OrderLogisticsTraceVO(
                "支付完成",
                "支付成功后商家会尽快为你发货",
                order.getPayTime(),
                order.getPayTime() != null && !canceled));
        traces.add(new OrderLogisticsTraceVO(
                "商家发货",
                deliveryDescription(order),
                deliveryTraceTime(order),
                deliveryTraceActive(order, canceled)));
        traces.add(new OrderLogisticsTraceVO(
                "确认收货",
                "商品已签收，交易完成",
                order.getReceiveTime(),
                order.getReceiveTime() != null && !canceled));
        if (canceled) {
            traces.add(new OrderLogisticsTraceVO("订单取消", "订单已取消，未继续履约", null, true));
        } else if (refunding || refunded) {
            traces.add(new OrderLogisticsTraceVO(
                    refunded ? "退款完成" : "退款处理中",
                    refunded ? "退款流程已完成" : "售后退款正在处理中",
                    null,
                    true));
        }
        return traces;
    }

    private String deliveryDescription(OmsOrder order) {
        if (StrUtil.isBlank(order.getDeliveryCompany()) && StrUtil.isBlank(order.getDeliveryNo())) {
            return "商家发货后会展示物流公司和单号";
        }
        StringBuilder desc = new StringBuilder();
        if (StrUtil.isNotBlank(order.getDeliveryCompany())) {
            desc.append(order.getDeliveryCompany());
        }
        if (StrUtil.isNotBlank(order.getDeliveryNo())) {
            if (desc.length() > 0) desc.append(" ");
            desc.append(order.getDeliveryNo());
        }
        if (StrUtil.isNotBlank(order.getDeliveryRemark())) {
            desc.append("，").append(order.getDeliveryRemark());
        }
        return desc.toString();
    }

    private boolean deliveryTraceActive(OmsOrder order, boolean canceled) {
        if (canceled) {
            return false;
        }
        if (order.getDeliveryTime() != null) {
            return true;
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        return status == OrderStatus.PENDING_RECEIVE || status == OrderStatus.COMPLETED;
    }

    private LocalDateTime deliveryTraceTime(OmsOrder order) {
        if (order.getDeliveryTime() != null) {
            return order.getDeliveryTime();
        }
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == OrderStatus.COMPLETED) {
            return order.getReceiveTime();
        }
        return null;
    }

    private String logisticsCurrentAction(OmsOrder order) {
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null) {
            return "订单履约中";
        }
        switch (status) {
            case PENDING_PAY:
                return "等待付款";
            case PENDING_DELIVERY:
                return "等待商家发货";
            case PENDING_RECEIVE:
                return "包裹运输中";
            case COMPLETED:
                return "交易已完成";
            case CANCELED:
                return "订单已取消";
            case REFUNDING:
                return "售后处理中";
            case REFUNDED:
                return "退款已完成";
            default:
                return status.getDesc();
        }
    }

    private String logisticsNextHint(OmsOrder order) {
        OrderStatus status = OrderStatus.of(order.getStatus());
        if (status == null) {
            return "请关注订单状态变化";
        }
        switch (status) {
            case PENDING_PAY:
                return "完成支付后，商家会进入发货流程";
            case PENDING_DELIVERY:
                return "商家会尽快打包发货，发货后这里会展示物流公司和单号";
            case PENDING_RECEIVE:
                return StrUtil.isBlank(order.getDeliveryNo())
                        ? "订单已发货，请留意包裹配送和签收"
                        : "请凭物流单号关注配送进度，收到商品后记得确认收货";
            case COMPLETED:
                return "订单已完成，可以评价商品或申请售后服务";
            case CANCELED:
                return "订单已取消，未进入后续配送流程";
            case REFUNDING:
                return "售后申请正在处理中，请关注退款进度";
            case REFUNDED:
                return "退款流程已完成，如有疑问可联系商家";
            default:
                return "请关注订单状态变化";
        }
    }

    private OrderVO detailVO(OmsOrder order) {
        List<OmsOrderItem> items = itemMapper.selectList(
                Wrappers.<OmsOrderItem>lambdaQuery().eq(OmsOrderItem::getOrderId, order.getId()));
        List<OrderItemVO> itemVOs = items.stream().map(this::toItemVO).collect(Collectors.toList());
        return toOrderVO(order, itemVOs);
    }

    @Override
    public OmsOrder getByIdOrThrow(Long orderId) {
        OmsOrder o = orderMapper.selectById(orderId);
        if (o == null) throw new BizException(ResultCode.ORDER_NOT_FOUND);
        return o;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long orderId, Integer payType) {
        OmsOrder order = getByIdOrThrow(orderId);
        if (OrderStatus.CANCELED.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_CANCELED);
        }
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            // 已经付过，幂等返回
            return;
        }
        // locked_stock -> sale_count
        List<OmsOrderItem> items = itemMapper.selectList(
                Wrappers.<OmsOrderItem>lambdaQuery().eq(OmsOrderItem::getOrderId, orderId));
        for (OmsOrderItem it : items) {
            skuService.consumeStock(it.getSkuId(), it.getQuantity());
            if (it.getSeckillSkuId() != null) {
                seckillService.onPaid(it.getSeckillSkuId(), it.getQuantity());
            }
        }
        // 核销优惠券
        if (order.getCouponUserId() != null) {
            couponService.use(order.getCouponUserId(), orderId);
        }
        OmsOrder upd = new OmsOrder();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.PENDING_DELIVERY.getCode());
        upd.setPayType(payType);
        upd.setPayTime(LocalDateTime.now());
        orderMapper.updateById(upd);
        log.info("[order] mark paid orderId={} payType={}", orderId, payType);
        notifyUser(order.getUserId(), "payment", "支付成功", "订单 " + order.getOrderNo() + " 已支付成功，商家将尽快为你发货。", "order", order.getId(), order.getOrderNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderVO deliver(Long orderId, AdminOrderDeliverDTO dto) {
        OmsOrder order = getByIdOrThrow(orderId);
        if (!OrderStatus.PENDING_DELIVERY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "仅待发货订单可发货");
        }
        String deliveryCompany = dto == null ? null : StrUtil.trim(dto.getDeliveryCompany());
        String deliveryNo = dto == null ? null : StrUtil.trim(dto.getDeliveryNo());
        String deliveryRemark = dto == null ? null : StrUtil.trim(dto.getDeliveryRemark());
        if (StrUtil.isBlank(deliveryCompany)) {
            throw new BizException(ResultCode.PARAM_MISSING, "物流公司不能为空");
        }
        if (StrUtil.isBlank(deliveryNo)) {
            throw new BizException(ResultCode.PARAM_MISSING, "物流单号不能为空");
        }
        OmsOrder upd = new OmsOrder();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.PENDING_RECEIVE.getCode());
        upd.setDeliveryTime(LocalDateTime.now());
        upd.setDeliveryCompany(StrUtil.sub(deliveryCompany, 0, 64));
        upd.setDeliveryNo(StrUtil.sub(deliveryNo, 0, 64));
        upd.setDeliveryRemark(StrUtil.isBlank(deliveryRemark) ? null : StrUtil.sub(deliveryRemark, 0, 255));
        orderMapper.updateById(upd);
        List<OmsOrderItem> items = itemMapper.selectList(
                Wrappers.<OmsOrderItem>lambdaQuery().eq(OmsOrderItem::getOrderId, orderId));
        notifyUser(order.getUserId(), "logistics", "订单已发货", "订单 " + order.getOrderNo() + " 已由 " + upd.getDeliveryCompany() + " 发出，物流单号：" + upd.getDeliveryNo() + "。", "order", order.getId(), order.getOrderNo());
        return toOrderVO(orderMapper.selectById(orderId),
                items.stream().map(this::toItemVO).collect(Collectors.toList()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmReceive(Long userId, Long orderId) {
        OmsOrder order = mustOwn(userId, orderId);
        if (!OrderStatus.PENDING_RECEIVE.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "仅待收货订单可确认");
        }
        OmsOrder upd = new OmsOrder();
        upd.setId(orderId);
        upd.setStatus(OrderStatus.COMPLETED.getCode());
        upd.setReceiveTime(LocalDateTime.now());
        orderMapper.updateById(upd);
        notifyUser(order.getUserId(), "order", "交易已完成", "订单 " + order.getOrderNo() + " 已确认收货，可以去评价商品或申请售后。", "order", order.getId(), order.getOrderNo());
    }

    private OmsOrder mustOwn(Long userId, Long orderId) {
        OmsOrder o = orderMapper.selectById(orderId);
        if (o == null || !o.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return o;
    }

    private OrderItemVO toItemVO(OmsOrderItem it) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtil.copyProperties(it, vo, "specData");
        if (StrUtil.isNotBlank(it.getSpecData())) {
            try {
                vo.setSpecData(JSON.parseObject(it.getSpecData(),
                        new TypeReference<Map<String, String>>() {}));
            } catch (Exception ignored) {}
        }
        return vo;
    }

    private OrderVO toOrderVO(OmsOrder o, List<OrderItemVO> items) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(o, vo, "receiverInfo");
        OrderStatus st = OrderStatus.of(o.getStatus());
        vo.setStatusDesc(st == null ? "未知" : st.getDesc());
        if (StrUtil.isNotBlank(o.getReceiverInfo())) {
            try {
                vo.setReceiver(JSON.parseObject(o.getReceiverInfo(), ReceiverVO.class));
            } catch (Exception ignored) {}
        }
        if (StrUtil.isNotBlank(o.getInvoiceInfo())) {
            try {
                vo.setInvoice(JSON.parseObject(o.getInvoiceInfo(), InvoiceVO.class));
            } catch (Exception ignored) {}
        }
        vo.setItems(items);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createSeckillOrder(Long userId, SmsSeckillSku seckillSku, Long addressId) {
        UmsUserAddress addr = addressService.getOne(userId, addressId);
        ReceiverVO receiver = new ReceiverVO();
        BeanUtil.copyProperties(addr, receiver);

        BigDecimal totalAmount = seckillSku.getSeckillPrice();
        FreightQuoteVO freightQuote = freightService.calculate(totalAmount, addr);
        BigDecimal freight = freightQuote.getFreightAmount();
        BigDecimal payAmount = totalAmount.add(freight);  // 秒杀无优惠券

        OmsOrder order = new OmsOrder();
        String orderNo = genOrderNo(userId);
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setPayAmount(payAmount);
        order.setFreightAmount(freight);
        order.setCouponAmount(BigDecimal.ZERO);
        order.setStatus(OrderStatus.PENDING_PAY.getCode());
        order.setReceiverInfo(JSON.toJSONString(receiver));
        order.setRemark("秒杀活动订单");
        orderMapper.insert(order);

        // 扣减正式库存（行级 update，防止 Redis 库存与 DB 不一致）
        PmsSku sku = skuService.getByIdOrThrow(seckillSku.getSkuId());
        boolean locked = skuService.lockStock(sku.getId(), 1);
        if (!locked) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH, "商品库存不足");
        }

        OmsOrderItem item = new OmsOrderItem();
        item.setOrderId(order.getId());
        item.setOrderNo(orderNo);
        item.setSpuId(seckillSku.getSpuId());
        item.setSkuId(seckillSku.getSkuId());
        item.setSeckillSkuId(seckillSku.getId());
        item.setSkuName(sku.getName());
        item.setSkuImage(sku.getImage());
        if (sku.getSpecData() != null) item.setSpecData(sku.getSpecData());
        item.setPrice(seckillSku.getSeckillPrice());
        item.setQuantity(1);
        item.setTotalAmount(seckillSku.getSeckillPrice());
        itemMapper.insert(item);

        try { timeoutProducer.send(order.getId()); } catch (Exception e) {
            log.warn("[seckill-order] send timeout MQ failed, orderId={}", order.getId(), e);
        }
        log.info("[seckill] order created orderNo={} userId={}", orderNo, userId);
        notifyUser(order.getUserId(), "order", "秒杀订单已提交", "秒杀订单 " + order.getOrderNo() + " 已提交，请及时完成支付。", "order", order.getId(), order.getOrderNo());
        return orderNo;
    }

    private void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId, String bizNo) {
        try {
            messageService.create(userId, type, title, content, bizType, bizId, bizNo);
        } catch (Exception ex) {
            log.warn("[message] create user message failed userId={} bizType={} bizId={}", userId, bizType, bizId, ex);
        }
    }

    private String genOrderNo(Long userId) {
        // 14 位时间 + 4 位用户尾号 + 4 位随机
        String ts = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", new Date());
        String tail = String.format("%04d", userId == null ? 0 : userId % 10000);
        String rnd = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return ts + tail + rnd;
    }
}
