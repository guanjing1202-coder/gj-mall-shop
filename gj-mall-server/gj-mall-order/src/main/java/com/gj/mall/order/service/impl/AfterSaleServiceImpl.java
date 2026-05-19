package com.gj.mall.order.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.AfterSaleApplyDTO;
import com.gj.mall.order.dto.AfterSaleReturnDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.service.AfterSaleService;
import com.gj.mall.order.vo.AfterSaleVO;
import com.gj.mall.user.service.UserMessageService;
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
public class AfterSaleServiceImpl implements AfterSaleService {

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
    private final UserMessageService messageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AfterSaleVO apply(Long userId, Long orderId, AfterSaleApplyDTO dto) {
        validateApplyDTO(dto);
        OmsOrder order = mustOwnOrder(userId, orderId);
        validateOrderCanAfterSale(order);

        Long activeCount = afterSaleMapper.selectCount(Wrappers.<OmsAfterSale>lambdaQuery()
                .eq(OmsAfterSale::getOrderId, order.getId())
                .eq(OmsAfterSale::getUserId, userId)
                .in(OmsAfterSale::getStatus, Arrays.asList(STATUS_PENDING, STATUS_WAIT_RETURN, STATUS_WAIT_REFUND)));
        if (activeCount != null && activeCount > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "该订单已有处理中的售后单");
        }

        BigDecimal amount = order.getPayAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.ORDER_AMOUNT_ERROR, "订单金额异常，暂不能申请售后");
        }

        OmsAfterSale afterSale = new OmsAfterSale();
        afterSale.setAfterSaleNo(genAfterSaleNo(userId));
        afterSale.setOrderId(order.getId());
        afterSale.setOrderNo(order.getOrderNo());
        afterSale.setUserId(userId);
        afterSale.setType(dto.getType());
        afterSale.setAmount(amount);
        afterSale.setReason(StrUtil.sub(StrUtil.trim(dto.getReason()), 0, 120));
        afterSale.setDescription(StrUtil.isBlank(dto.getDescription()) ? null : StrUtil.sub(StrUtil.trim(dto.getDescription()), 0, 1000));
        afterSale.setImages(CollUtil.isEmpty(dto.getImages()) ? null : JSON.toJSONString(dto.getImages()));
        afterSale.setOrderStatusSnapshot(order.getStatus());
        afterSale.setStatus(STATUS_PENDING);
        afterSaleMapper.insert(afterSale);

        updateOrderStatus(order.getId(), OrderStatus.REFUNDING.getCode());
        notifyUser(userId, "after_sale", "售后申请已提交", "售后单 " + afterSale.getAfterSaleNo() + " 已提交，商家会尽快审核。", "after_sale", afterSale.getId(), afterSale.getAfterSaleNo());
        return enrich(Collections.singletonList(afterSale)).get(0);
    }

    @Override
    public List<AfterSaleVO> listByOrder(Long userId, Long orderId) {
        mustOwnOrder(userId, orderId);
        List<OmsAfterSale> afterSales = afterSaleMapper.selectList(Wrappers.<OmsAfterSale>lambdaQuery()
                .eq(OmsAfterSale::getUserId, userId)
                .eq(OmsAfterSale::getOrderId, orderId)
                .orderByDesc(OmsAfterSale::getCreateTime));
        return enrich(afterSales);
    }

    @Override
    public PageResult<AfterSaleVO> page(Long userId, OrderQueryDTO query) {
        long pageNum = query == null || query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query == null || query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        Integer status = query == null ? null : query.getStatus();
        Page<OmsAfterSale> result = afterSaleMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<OmsAfterSale>lambdaQuery()
                        .eq(OmsAfterSale::getUserId, userId)
                        .eq(status != null, OmsAfterSale::getStatus, status)
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
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long userId, Long id) {
        OmsAfterSale afterSale = mustOwnAfterSale(userId, id);
        if (Integer.valueOf(STATUS_COMPLETED).equals(afterSale.getStatus())
                || Integer.valueOf(STATUS_REJECTED).equals(afterSale.getStatus())
                || Integer.valueOf(STATUS_CANCELED).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "当前售后单不可取消");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setStatus(STATUS_CANCELED);
        afterSaleMapper.updateById(update);
        restoreOrderStatus(afterSale);
        notifyUser(userId, "after_sale", "售后申请已取消", "售后单 " + afterSale.getAfterSaleNo() + " 已取消，订单状态已恢复。", "after_sale", afterSale.getId(), afterSale.getAfterSaleNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitReturn(Long userId, Long id, AfterSaleReturnDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        OmsAfterSale afterSale = mustOwnAfterSale(userId, id);
        if (!Integer.valueOf(STATUS_WAIT_RETURN).equals(afterSale.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待退货售后单可填写退货物流");
        }
        OmsAfterSale update = new OmsAfterSale();
        update.setId(id);
        update.setReturnCompany(StrUtil.sub(StrUtil.trim(dto.getReturnCompany()), 0, 64));
        update.setReturnNo(StrUtil.sub(StrUtil.trim(dto.getReturnNo()), 0, 64));
        afterSaleMapper.updateById(update);
        notifyUser(userId, "after_sale", "退货物流已提交", "售后单 " + afterSale.getAfterSaleNo() + " 已提交退货物流，等待商家确认收货。", "after_sale", afterSale.getId(), afterSale.getAfterSaleNo());
    }

    private void validateApplyDTO(AfterSaleApplyDTO dto) {
        if (dto == null) {
            throw new BizException(ResultCode.PARAM_MISSING);
        }
        if (!Integer.valueOf(TYPE_REFUND_ONLY).equals(dto.getType())
                && !Integer.valueOf(TYPE_RETURN_REFUND).equals(dto.getType())) {
            throw new BizException(ResultCode.PARAM_ERROR, "售后类型非法");
        }
        if (StrUtil.isBlank(dto.getReason())) {
            throw new BizException(ResultCode.PARAM_MISSING, "售后原因不能为空");
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

    private OmsOrder mustOwnOrder(Long userId, Long orderId) {
        OmsOrder order = orderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private OmsAfterSale mustOwnAfterSale(Long userId, Long id) {
        OmsAfterSale afterSale = afterSaleMapper.selectById(id);
        if (afterSale == null || !Objects.equals(afterSale.getUserId(), userId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "售后单不存在");
        }
        return afterSale;
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

    private List<AfterSaleVO> enrich(List<OmsAfterSale> afterSales) {
        if (CollUtil.isEmpty(afterSales)) {
            return Collections.emptyList();
        }
        List<Long> orderIds = afterSales.stream().map(OmsAfterSale::getOrderId).distinct().collect(Collectors.toList());
        Map<Long, List<OmsOrderItem>> itemMap = orderItemMapper.selectList(
                        Wrappers.<OmsOrderItem>lambdaQuery().in(OmsOrderItem::getOrderId, orderIds))
                .stream()
                .collect(Collectors.groupingBy(OmsOrderItem::getOrderId));
        return afterSales.stream()
                .map(item -> AfterSaleVO.from(item, itemMap.getOrDefault(item.getOrderId(), Collections.emptyList())))
                .collect(Collectors.toList());
    }

    private String genAfterSaleNo(Long userId) {
        String ts = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", new Date());
        String tail = String.format("%04d", userId == null ? 0 : userId % 10000);
        String rnd = String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
        return "AS" + ts + tail + rnd;
    }

    private void notifyUser(Long userId, String type, String title, String content, String bizType, Long bizId, String bizNo) {
        try {
            messageService.create(userId, type, title, content, bizType, bizId, bizNo);
        } catch (Exception ignored) {
        }
    }
}
