package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminAfterSaleActionDTO;
import com.gj.mall.admin.dto.AdminAfterSaleCreateDTO;
import com.gj.mall.admin.dto.AdminAfterSaleQueryDTO;
import com.gj.mall.admin.service.AdminAfterSaleService;
import com.gj.mall.admin.vo.AdminAfterSaleVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminAfterSaleServiceImpl implements AdminAfterSaleService {

    private static final int STATUS_PENDING = 0;
    private static final int STATUS_WAIT_RETURN = 1;
    private static final int STATUS_WAIT_REFUND = 2;
    private static final int STATUS_REJECTED = 3;
    private static final int STATUS_COMPLETED = 4;
    private static final int STATUS_CANCELED = 5;

    private static final int TYPE_REFUND_ONLY = 1;
    private static final int TYPE_RETURN_REFUND = 2;

    private final OmsAfterSaleMapper afterSaleMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper orderItemMapper;
    private final PayPaymentRecordMapper paymentRecordMapper;
    private final UmsUserMapper userMapper;

    @Override
    public PageResult<AdminAfterSaleVO> page(AdminAfterSaleQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        IPage<OmsAfterSale> result = afterSaleMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<OmsAfterSale>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(OmsAfterSale::getAfterSaleNo, query.getKeyword())
                                .or()
                                .like(OmsAfterSale::getOrderNo, query.getKeyword())
                                .or()
                                .like(OmsAfterSale::getReason, query.getKeyword()))
                        .eq(query.getUserId() != null, OmsAfterSale::getUserId, query.getUserId())
                        .eq(query.getType() != null, OmsAfterSale::getType, query.getType())
                        .eq(query.getStatus() != null, OmsAfterSale::getStatus, query.getStatus())
                        .orderByDesc(OmsAfterSale::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                enrich(result.getRecords()));
    }

    @Override
    public AdminAfterSaleVO detail(Long id) {
        return enrich(Collections.singletonList(getByIdOrThrow(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(AdminAfterSaleCreateDTO dto) {
        validateCreateDTO(dto);
        OmsOrder order = getOrderOrThrow(dto.getOrderId());
        validateOrderCanAfterSale(order);
        Long activeCount = afterSaleMapper.selectCount(Wrappers.<OmsAfterSale>lambdaQuery()
                .eq(OmsAfterSale::getOrderId, order.getId())
                .in(OmsAfterSale::getStatus, Arrays.asList(STATUS_PENDING, STATUS_WAIT_RETURN, STATUS_WAIT_REFUND)));
        if (activeCount != null && activeCount > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "该订单已有处理中的售后单");
        }

        BigDecimal amount = dto.getAmount() == null ? order.getPayAmount() : dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "售后金额必须大于0");
        }
        if (order.getPayAmount() == null || amount.compareTo(order.getPayAmount()) != 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "当前版本仅支持全额售后退款");
        }

        OmsAfterSale afterSale = new OmsAfterSale();
        afterSale.setAfterSaleNo(genAfterSaleNo(order.getUserId()));
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(order.getUserId());
        afterSale.setType(dto.getType());
        afterSale.setAmount(amount);
        afterSale.setReason(StrUtil.blankToDefault(dto.getReason(), "后台发起售后"));
        afterSale.setDescription(dto.getDescription());
        afterSale.setImages(CollUtil.isEmpty(dto.getImages()) ? null : JSON.toJSONString(dto.getImages()));
        afterSale.setOrderStatusSnapshot(order.getStatus());
        afterSale.setStatus(STATUS_PENDING);
        afterSaleMapper.insert(afterSale);

        updateOrderStatus(order.getId(), OrderStatus.REFUNDING.getCode());
        return afterSale.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void approve(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = getByIdOrThrow(id);
        if (!Integer.valueOf(STATUS_PENDING).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待审核售后单可审核通过");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(Integer.valueOf(TYPE_RETURN_REFUND).equals(afterSale.getType())
                ? STATUS_WAIT_RETURN
                : STATUS_WAIT_REFUND);
        update.setAuditRemark(dto == null ? null : dto.getAuditRemark());
        update.setAuditTime(LocalDateTime.now());
        afterSaleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reject(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = getByIdOrThrow(id);
        if (!Integer.valueOf(STATUS_PENDING).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待审核售后单可拒绝");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(STATUS_REJECTED);
        update.setRejectReason(dto == null ? null : dto.getRejectReason());
        update.setAuditRemark(dto == null ? null : dto.getAuditRemark());
        update.setAuditTime(LocalDateTime.now());
        afterSaleMapper.updateById(update);
        restoreOrderStatus(afterSale);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void receive(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = getByIdOrThrow(id);
        if (!Integer.valueOf(STATUS_WAIT_RETURN).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待退货售后单可确认收货");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(STATUS_WAIT_REFUND);
        update.setReturnCompany(dto == null ? null : dto.getReturnCompany());
        update.setReturnNo(dto == null ? null : dto.getReturnNo());
        update.setReceiveTime(LocalDateTime.now());
        afterSaleMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = getByIdOrThrow(id);
        if (!Integer.valueOf(STATUS_WAIT_REFUND).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待退款售后单可退款");
        }
        OmsOrder order = getOrderOrThrow(afterSale.getOrderId());
        PayPaymentRecord record = paymentRecordMapper.selectOne(Wrappers.<PayPaymentRecord>lambdaQuery()
                .eq(PayPaymentRecord::getOrderId, order.getId())
                .eq(PayPaymentRecord::getStatus, 1)
                .orderByDesc(PayPaymentRecord::getCreateTime)
                .last("LIMIT 1"));
        if (record == null) {
            throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND, "未找到可退款的支付流水");
        }
        if (afterSale.getAmount() == null || record.getAmount() == null
                || afterSale.getAmount().compareTo(record.getAmount()) != 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "当前版本仅支持全额退款");
        }

        PayPaymentRecord paymentUpdate = new PayPaymentRecord();
        paymentUpdate.setId(record.getId());
        paymentUpdate.setStatus(3);
        paymentUpdate.setCallbackData(appendCallback(record.getCallbackData(),
                "after-sale-refund afterSaleNo=" + afterSale.getAfterSaleNo()
                        + " amount=" + afterSale.getAmount()
                        + " remark=" + (dto == null ? "-" : StrUtil.blankToDefault(dto.getAuditRemark(), "-"))));
        paymentRecordMapper.updateById(paymentUpdate);

        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(STATUS_COMPLETED);
        update.setRefundPaymentId(record.getId());
        update.setAuditRemark(dto == null ? afterSale.getAuditRemark() : dto.getAuditRemark());
        update.setRefundTime(LocalDateTime.now());
        afterSaleMapper.updateById(update);
        updateOrderStatus(order.getId(), OrderStatus.REFUNDED.getCode());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long id, AdminAfterSaleActionDTO dto) {
        OmsAfterSale afterSale = getByIdOrThrow(id);
        if (Integer.valueOf(STATUS_COMPLETED).equals(afterSale.getStatus())
                || Integer.valueOf(STATUS_REJECTED).equals(afterSale.getStatus())
                || Integer.valueOf(STATUS_CANCELED).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "当前售后单不可取消");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(STATUS_CANCELED);
        update.setAuditRemark(dto == null ? null : dto.getAuditRemark());
        afterSaleMapper.updateById(update);
        restoreOrderStatus(afterSale);
    }

    private OmsAfterSale getByIdOrThrow(Long id) {
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "售后单不存在");
        }
        return afterSale;
    }

    private OmsOrder getOrderOrThrow(Long id) {
        OmsOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private void validateCreateDTO(AdminAfterSaleCreateDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        if (!Integer.valueOf(TYPE_REFUND_ONLY).equals(dto.getType())
                && !Integer.valueOf(TYPE_RETURN_REFUND).equals(dto.getType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "售后类型非法");
        }
    }

    private void validateOrderCanAfterSale(OmsOrder order) {
        if (OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())
                || OrderStatus.CANCELED.getCode().equals(order.getStatus())
                || OrderStatus.REFUNDING.getCode().equals(order.getStatus())
                || OrderStatus.REFUNDED.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不可发起售后");
        }
    }

    private void updateOrderStatus(Long orderId, Integer status) {
        OmsOrder update = new OmsOrder();
        update.setId(orderId);
        update.setStatus(status);
        orderMapper.updateById(update);
    }

    private void restoreOrderStatus(OmsAfterSale afterSale) {
        if (afterSale.getOrderStatusSnapshot() == null) {
            return;
        }
        updateOrderStatus(afterSale.getOrderId(), afterSale.getOrderStatusSnapshot());
    }

    private List<AdminAfterSaleVO> enrich(List<OmsAfterSale> afterSales) {
        List<Long> orderIds = afterSales.stream().map(OmsAfterSale::getOrderId).distinct().collect(Collectors.toList());
        Map<Long, OmsOrder> orderMap = CollUtil.isEmpty(orderIds)
                ? Collections.emptyMap()
                : orderMapper.selectList(Wrappers.<OmsOrder>lambdaQuery().in(OmsOrder::getId, orderIds))
                        .stream()
                        .collect(Collectors.toMap(OmsOrder::getId, item -> item, (a, b) -> a));

        Map<Long, List<OmsOrderItem>> itemMap = CollUtil.isEmpty(orderIds)
                ? Collections.emptyMap()
                : orderItemMapper.selectList(Wrappers.<OmsOrderItem>lambdaQuery().in(OmsOrderItem::getOrderId, orderIds))
                        .stream()
                        .collect(Collectors.groupingBy(OmsOrderItem::getOrderId));

        List<Long> userIds = afterSales.stream().map(OmsAfterSale::getUserId).distinct().collect(Collectors.toList());
        Map<Long, UmsUser> userMap = CollUtil.isEmpty(userIds)
                ? Collections.emptyMap()
                : userMapper.selectList(Wrappers.<UmsUser>lambdaQuery().in(UmsUser::getId, userIds))
                        .stream()
                        .collect(Collectors.toMap(UmsUser::getId, item -> item, (a, b) -> a));

        return afterSales.stream()
                .map(item -> AdminAfterSaleVO.from(
                        item,
                        orderMap.get(item.getOrderId()),
                        userMap.get(item.getUserId()),
                        itemMap.getOrDefault(item.getOrderId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private String genAfterSaleNo(Long userId) {
        String ts = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", new Date());
        String tail = String.format("%04d", userId == null ? 0 : userId % 10000);
        String rnd = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return "AS" + ts + tail + rnd;
    }

    private String appendCallback(String source, String text) {
        String line = "[" + LocalDateTime.now() + "] " + text;
        return StrUtil.isBlank(source) ? line : source + "\n" + line;
    }
}
