package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminPaymentQueryDTO;
import com.gj.mall.admin.dto.AdminPaymentRefundDTO;
import com.gj.mall.admin.service.AdminPaymentService;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PayPaymentRecordMapper recordMapper;
    private final OmsOrderMapper orderMapper;
    private final UmsUserMapper userMapper;
    private final OrderService orderService;

    @Override
    public PageResult<AdminPaymentVO> page(AdminPaymentQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        IPage<PayPaymentRecord> result = recordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<PayPaymentRecord>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(PayPaymentRecord::getPayNo, query.getKeyword())
                                .or()
                                .like(PayPaymentRecord::getOrderNo, query.getKeyword())
                                .or()
                                .like(PayPaymentRecord::getThirdPayNo, query.getKeyword()))
                        .eq(query.getUserId() != null, PayPaymentRecord::getUserId, query.getUserId())
                        .eq(query.getChannel() != null, PayPaymentRecord::getChannel, query.getChannel())
                        .eq(query.getStatus() != null, PayPaymentRecord::getStatus, query.getStatus())
                        .orderByDesc(PayPaymentRecord::getCreateTime));
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
    public AdminPaymentVO detail(Long id) {
        return enrich(Collections.singletonList(getByIdOrThrow(id))).get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markPaid(Long id, String thirdPayNo) {
        PayPaymentRecord record = getByIdOrThrow(id);
        if (Integer.valueOf(3).equals(record.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "已退款记录不能同步为已支付");
        }
        if (Integer.valueOf(1).equals(record.getStatus())) {
            return;
        }
        PayPaymentRecord update = new PayPaymentRecord();
        update.setId(id);
        update.setStatus(1);
        update.setThirdPayNo(StrUtil.blankToDefault(thirdPayNo, "ADMIN-" + record.getPayNo()));
        update.setPayTime(LocalDateTime.now());
        update.setCallbackData(appendCallback(record.getCallbackData(), "admin-mark-paid"));
        recordMapper.updateById(update);
        orderService.markPaid(record.getOrderId(), record.getChannel());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markFailed(Long id, String reason) {
        PayPaymentRecord record = getByIdOrThrow(id);
        if (!Integer.valueOf(0).equals(record.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅待支付记录可标记失败");
        }
        PayPaymentRecord update = new PayPaymentRecord();
        update.setId(id);
        update.setStatus(2);
        update.setCallbackData(appendCallback(record.getCallbackData(),
                "admin-mark-failed reason=" + StrUtil.blankToDefault(reason, "-")));
        recordMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long id, AdminPaymentRefundDTO dto) {
        PayPaymentRecord record = getByIdOrThrow(id);
        if (!Integer.valueOf(1).equals(record.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅已支付记录可退款");
        }
        OmsOrder order = orderMapper.selectById(record.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (OrderStatus.CANCELED.getCode().equals(order.getStatus())
                || OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_STATUS_ERROR, "当前订单状态不可退款");
        }
        BigDecimal amount = dto == null || dto.getAmount() == null ? record.getAmount() : dto.getAmount();
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "退款金额必须大于0");
        }
        if (amount.compareTo(record.getAmount()) != 0) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "当前版本仅支持全额退款");
        }

        PayPaymentRecord update = new PayPaymentRecord();
        update.setId(id);
        update.setStatus(3);
        update.setCallbackData(appendCallback(record.getCallbackData(),
                "admin-refund amount=" + amount
                        + " reason=" + (dto == null ? "-" : StrUtil.blankToDefault(dto.getReason(), "-"))));
        recordMapper.updateById(update);

        OmsOrder orderUpdate = new OmsOrder();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(orderUpdate);
    }

    private PayPaymentRecord getByIdOrThrow(Long id) {
        PayPaymentRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND);
        }
        return record;
    }

    private List<AdminPaymentVO> enrich(List<PayPaymentRecord> records) {
        List<Long> orderIds = records.stream()
                .map(PayPaymentRecord::getOrderId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, OmsOrder> orderMap = CollUtil.isEmpty(orderIds)
                ? Collections.emptyMap()
                : orderMapper.selectList(Wrappers.<OmsOrder>lambdaQuery().in(OmsOrder::getId, orderIds))
                        .stream()
                        .collect(Collectors.toMap(OmsOrder::getId, item -> item, (a, b) -> a));

        List<Long> userIds = records.stream()
                .map(PayPaymentRecord::getUserId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, UmsUser> userMap = CollUtil.isEmpty(userIds)
                ? Collections.emptyMap()
                : userMapper.selectList(Wrappers.<UmsUser>lambdaQuery().in(UmsUser::getId, userIds))
                        .stream()
                        .collect(Collectors.toMap(UmsUser::getId, item -> item, (a, b) -> a));

        return records.stream()
                .map(item -> AdminPaymentVO.from(item, orderMap.get(item.getOrderId()), userMap.get(item.getUserId())))
                .collect(Collectors.toList());
    }

    private String appendCallback(String source, String text) {
        String line = "[" + LocalDateTime.now() + "] " + text;
        return StrUtil.isBlank(source) ? line : source + "\n" + line;
    }
}
