package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.AdminPaymentCallbackQueryDTO;
import com.gj.mall.admin.dto.AdminPaymentQueryDTO;
import com.gj.mall.admin.dto.AdminPaymentRefundDTO;
import com.gj.mall.admin.service.AdminPaymentService;
import com.gj.mall.admin.vo.AdminPaymentAccessVO;
import com.gj.mall.admin.vo.AdminPaymentCallbackVO;
import com.gj.mall.admin.vo.AdminPaymentSummaryVO;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.entity.PayRefundRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.mapper.PayRefundRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminPaymentServiceImpl implements AdminPaymentService {

    private final PayPaymentRecordMapper recordMapper;
    private final PayCallbackRecordMapper callbackRecordMapper;
    private final PayRefundRecordMapper refundRecordMapper;
    private final OmsOrderMapper orderMapper;
    private final UmsUserMapper userMapper;
    private final OrderService orderService;
    private final PayService payService;
    private final PayCallbackSignatureSupport signatureSupport;
    private final PayCallbackProperties callbackProperties;

    @Value("${mall.pay.mode:mock}")
    private String payMode;

    @Value("${mall.pay.callback.require-signature:false}")
    private Boolean requireSignature;

    @Override
    public AdminPaymentSummaryVO summary() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        AdminPaymentSummaryVO vo = new AdminPaymentSummaryVO();
        vo.setTotalCount(countByStatus(null));
        vo.setTotalAmount(sumAmountByStatus(null, null, null));
        vo.setPendingCount(countByStatus(0));
        vo.setPendingAmount(sumAmountByStatus(0, null, null));
        vo.setPaidCount(countByStatus(1));
        vo.setPaidAmount(sumAmountByStatus(1, null, null));
        vo.setFailedCount(countByStatus(2));
        vo.setFailedAmount(sumAmountByStatus(2, null, null));
        vo.setRefundedCount(countByStatus(3));
        vo.setRefundedAmount(sumAmountByStatus(3, null, null));
        vo.setTodayPaidAmount(sumAmountByStatus(1, todayStart, tomorrowStart));
        vo.setTodayRefundedAmount(sumAmountByStatus(3, todayStart, tomorrowStart));
        Long successCount = safeLong(vo.getPaidCount()) + safeLong(vo.getRefundedCount());
        vo.setSuccessRate(percent(successCount, vo.getTotalCount()));
        vo.setRefundRate(percent(vo.getRefundedCount(), successCount));
        vo.setChannels(buildChannelSummary());
        return vo;
    }

    @Override
    public AdminPaymentAccessVO access() {
        AdminPaymentAccessVO vo = new AdminPaymentAccessVO();
        vo.setMode(payMode);
        vo.setCallbackRequireSignature(Boolean.TRUE.equals(requireSignature));
        vo.setCallbackPath("/api/pay/callback/{channel}");
        vo.setDevSignatureAlgorithm("HmacSHA256(sortedPayload, mall.pay.callback.secret)");
        fillDevSignatureSample(vo);
        boolean realMode = "real".equalsIgnoreCase(payMode);
        vo.getChannels().add(AdminPaymentAccessVO.ChannelItem.of(
                PayChannel.MOCK.getCode(), PayChannel.MOCK.getName(), PayChannel.MOCK.getDesc(), true, "开发环境即时成功"));
        vo.getChannels().add(AdminPaymentAccessVO.ChannelItem.of(
                PayChannel.WECHAT.getCode(), PayChannel.WECHAT.getName(), PayChannel.WECHAT.getDesc(), realMode, realMode ? "待接入商户配置" : "未启用"));
        vo.getChannels().add(AdminPaymentAccessVO.ChannelItem.of(
                PayChannel.ALIPAY.getCode(), PayChannel.ALIPAY.getName(), PayChannel.ALIPAY.getDesc(), realMode, realMode ? "待接入应用配置" : "未启用"));
        return vo;
    }

    @Override
    public PageResult<AdminPaymentCallbackVO> callbackPage(AdminPaymentCallbackQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : Math.min(query.getPageSize(), 50);
        IPage<PayCallbackRecord> result = callbackRecordMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<PayCallbackRecord>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(PayCallbackRecord::getCallbackNo, query.getKeyword())
                                .or()
                                .like(PayCallbackRecord::getPayNo, query.getKeyword())
                                .or()
                                .like(PayCallbackRecord::getThirdPayNo, query.getKeyword())
                                .or()
                                .like(PayCallbackRecord::getNotifyId, query.getKeyword()))
                        .eq(query.getChannel() != null, PayCallbackRecord::getChannel, query.getChannel())
                        .eq(query.getSignatureStatus() != null, PayCallbackRecord::getSignatureStatus, query.getSignatureStatus())
                        .eq(query.getProcessStatus() != null, PayCallbackRecord::getProcessStatus, query.getProcessStatus())
                        .orderByDesc(PayCallbackRecord::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(AdminPaymentCallbackVO::from).collect(Collectors.toList()));
    }

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
    public void syncStatus(Long id) {
        PayPaymentRecord record = getByIdOrThrow(id);
        if (Integer.valueOf(1).equals(record.getStatus())) {
            return;
        }
        if (Integer.valueOf(3).equals(record.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "已退款记录不能同步支付状态");
        }
        PayCallbackRecord callback = latestSuccessfulCallback(record);
        if (callback == null) {
            throw new BizException(ResultCode.PAY_FAIL, "未找到可入账的成功支付回调");
        }
        PayPaymentRecord update = new PayPaymentRecord();
        update.setId(record.getId());
        update.setStatus(1);
        update.setThirdPayNo(StrUtil.blankToDefault(callback.getThirdPayNo(), record.getThirdPayNo()));
        update.setPayTime(LocalDateTime.now());
        update.setCallbackData(appendCallback(record.getCallbackData(),
                "admin-sync-status callbackNo=" + callback.getCallbackNo()));
        recordMapper.updateById(update);
        orderService.markPaid(record.getOrderId(), record.getChannel());
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
        PayRefundRecord existingRefund = refundRecordMapper.selectOne(Wrappers.<PayRefundRecord>lambdaQuery()
                .eq(PayRefundRecord::getPaymentId, record.getId())
                .last("LIMIT 1"));
        if (existingRefund != null) {
            throw new BizException(ResultCode.DATA_EXISTS, "退款记录已存在，请勿重复退款");
        }
        String reason = dto == null ? null : StrUtil.trim(dto.getReason());
        LocalDateTime now = LocalDateTime.now();

        PayRefundRecord refundRecord = new PayRefundRecord();
        refundRecord.setRefundNo(genRefundNo(record.getUserId()));
        refundRecord.setPaymentId(record.getId());
        refundRecord.setPayNo(record.getPayNo());
        refundRecord.setThirdPayNo(record.getThirdPayNo());
        refundRecord.setOrderId(record.getOrderId());
        refundRecord.setOrderNo(record.getOrderNo());
        refundRecord.setUserId(record.getUserId());
        refundRecord.setChannel(record.getChannel());
        refundRecord.setAmount(amount);
        refundRecord.setStatus(1);
        refundRecord.setReason(StrUtil.blankToDefault(reason, "后台支付退款"));
        refundRecord.setOperatorType("admin_payment");
        refundRecord.setCallbackData("admin-payment-refund paymentId=" + record.getId()
                + " amount=" + amount);
        refundRecord.setSuccessTime(now);
        refundRecordMapper.insert(refundRecord);

        PayPaymentRecord update = new PayPaymentRecord();
        update.setId(id);
        update.setStatus(3);
        update.setCallbackData(appendCallback(record.getCallbackData(),
                "admin-refund amount=" + amount
                        + " refundNo=" + refundRecord.getRefundNo()
                        + " reason=" + StrUtil.blankToDefault(reason, "-")));
        recordMapper.updateById(update);

        OmsOrder orderUpdate = new OmsOrder();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(orderUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void retryRefund(Long refundId) {
        PayRefundRecord refundRecord = getRefundByIdOrThrow(refundId);
        if (!Integer.valueOf(0).equals(refundRecord.getStatus())
                && !Integer.valueOf(2).equals(refundRecord.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅退款中或退款失败记录可重试");
        }
        PayPaymentRecord payment = getByIdOrThrow(refundRecord.getPaymentId());
        OmsOrder order = orderMapper.selectById(refundRecord.getOrderId());
        if (order == null) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();

        PayRefundRecord refundUpdate = new PayRefundRecord();
        refundUpdate.setId(refundRecord.getId());
        refundUpdate.setStatus(1);
        refundUpdate.setSuccessTime(now);
        refundUpdate.setCallbackData(appendCallback(refundRecord.getCallbackData(),
                "admin-retry-refund success paymentId=" + payment.getId()));
        refundRecordMapper.updateById(refundUpdate);

        PayPaymentRecord paymentUpdate = new PayPaymentRecord();
        paymentUpdate.setId(payment.getId());
        paymentUpdate.setStatus(3);
        paymentUpdate.setCallbackData(appendCallback(payment.getCallbackData(),
                "admin-retry-refund refundNo=" + refundRecord.getRefundNo()));
        recordMapper.updateById(paymentUpdate);

        OmsOrder orderUpdate = new OmsOrder();
        orderUpdate.setId(order.getId());
        orderUpdate.setStatus(OrderStatus.REFUNDED.getCode());
        orderMapper.updateById(orderUpdate);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRefundFailed(Long refundId, String reason) {
        PayRefundRecord refundRecord = getRefundByIdOrThrow(refundId);
        if (!Integer.valueOf(0).equals(refundRecord.getStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅退款中记录可标记失败");
        }
        PayRefundRecord refundUpdate = new PayRefundRecord();
        refundUpdate.setId(refundRecord.getId());
        refundUpdate.setStatus(2);
        refundUpdate.setCallbackData(appendCallback(refundRecord.getCallbackData(),
                "admin-mark-refund-failed reason=" + StrUtil.blankToDefault(StrUtil.trim(reason), "-")));
        refundRecordMapper.updateById(refundUpdate);
    }

    @Override
    public void replayCallback(Long callbackId) {
        payService.replayCallback(callbackId);
    }

    private PayPaymentRecord getByIdOrThrow(Long id) {
        PayPaymentRecord record = recordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND);
        }
        return record;
    }

    private PayRefundRecord getRefundByIdOrThrow(Long id) {
        PayRefundRecord record = refundRecordMapper.selectById(id);
        if (record == null) {
            throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND, "退款记录不存在");
        }
        return record;
    }

    private PayCallbackRecord latestSuccessfulCallback(PayPaymentRecord record) {
        List<PayCallbackRecord> callbacks = callbackRecordMapper.selectList(Wrappers.<PayCallbackRecord>lambdaQuery()
                .eq(PayCallbackRecord::getPayNo, record.getPayNo())
                .eq(PayCallbackRecord::getChannel, record.getChannel())
                .eq(PayCallbackRecord::getProcessStatus, 1)
                .in(PayCallbackRecord::getSignatureStatus, 0, 1)
                .orderByDesc(PayCallbackRecord::getCreateTime)
                .last("LIMIT 1"));
        return CollUtil.isEmpty(callbacks) ? null : callbacks.get(0);
    }

    private Long countByStatus(Integer status) {
        return recordMapper.selectCount(Wrappers.<PayPaymentRecord>lambdaQuery()
                .eq(status != null, PayPaymentRecord::getStatus, status));
    }

    private BigDecimal sumAmountByStatus(Integer status, LocalDateTime start, LocalDateTime end) {
        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PayPaymentRecord> wrapper = Wrappers.query();
        wrapper.select("COALESCE(SUM(amount), 0)");
        if (status != null) {
            wrapper.eq("status", status);
        }
        if (start != null) {
            wrapper.ge("update_time", start);
        }
        if (end != null) {
            wrapper.lt("update_time", end);
        }
        List<Object> rows = recordMapper.selectObjs(wrapper);
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        Object value = rows.get(0);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private List<AdminPaymentSummaryVO.ChannelItem> buildChannelSummary() {
        Map<Integer, AdminPaymentSummaryVO.ChannelItem> channelMap = Arrays.stream(PayChannel.values())
                .map(AdminPaymentSummaryVO.ChannelItem::empty)
                .collect(Collectors.toMap(AdminPaymentSummaryVO.ChannelItem::getChannel, item -> item,
                        (a, b) -> a, LinkedHashMap::new));

        com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<PayPaymentRecord> wrapper = Wrappers.query();
        wrapper.select("channel, COUNT(*) AS count, COALESCE(SUM(amount), 0) AS amount");
        wrapper.groupBy("channel");
        List<Map<String, Object>> rows = recordMapper.selectMaps(wrapper);
        for (Map<String, Object> row : rows) {
            Integer channel = toInteger(row.get("channel"));
            if (channel == null) {
                continue;
            }
            AdminPaymentSummaryVO.ChannelItem item = channelMap.computeIfAbsent(channel, key -> {
                AdminPaymentSummaryVO.ChannelItem unknown = new AdminPaymentSummaryVO.ChannelItem();
                unknown.setChannel(key);
                unknown.setChannelDesc("未知渠道");
                return unknown;
            });
            item.setCount(toLong(row.get("count")));
            item.setAmount(toBigDecimal(row.get("amount")));
        }
        return new ArrayList<>(channelMap.values());
    }

    private BigDecimal percent(Long numerator, Long denominator) {
        long down = denominator == null ? 0L : denominator;
        if (down <= 0) {
            return BigDecimal.ZERO;
        }
        long up = numerator == null ? 0L : numerator;
        return BigDecimal.valueOf(up)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(down), 2, RoundingMode.HALF_UP);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.parseLong(value.toString());
    }

    private Integer toInteger(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
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
        Map<Long, PayRefundRecord> refundMap = refundRecordMap(records);

        return records.stream()
                .map(item -> AdminPaymentVO.from(item, orderMap.get(item.getOrderId()), userMap.get(item.getUserId()), refundMap.get(item.getId())))
                .collect(Collectors.toList());
    }

    private Map<Long, PayRefundRecord> refundRecordMap(List<PayPaymentRecord> records) {
        List<Long> paymentIds = records.stream()
                .map(PayPaymentRecord::getId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(paymentIds)) {
            return Collections.emptyMap();
        }
        return refundRecordMapper.selectList(Wrappers.<PayRefundRecord>lambdaQuery()
                        .in(PayRefundRecord::getPaymentId, paymentIds)
                        .orderByDesc(PayRefundRecord::getCreateTime))
                .stream()
                .collect(Collectors.toMap(PayRefundRecord::getPaymentId, item -> item, (a, b) -> a));
    }

    private String appendCallback(String source, String text) {
        String line = "[" + LocalDateTime.now() + "] " + text;
        return StrUtil.isBlank(source) ? line : source + "\n" + line;
    }

    private String genRefundNo(Long userId) {
        String ts = String.format("%1$tY%1$tm%1$td%1$tH%1$tM%1$tS", new Date());
        String tail = String.format("%04d", userId == null ? 0 : userId % 10000);
        String rnd = String.format("%04d", new Random().nextInt(10000));
        return "RF" + ts + tail + rnd;
    }

    private void fillDevSignatureSample(AdminPaymentAccessVO vo) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("amount", "99.00");
        payload.put("eventType", "SUCCESS");
        payload.put("notifyId", "N202605220001");
        payload.put("payNo", "P202605220001");
        payload.put("thirdPayNo", "MOCK-P202605220001");
        String signature = signatureSupport.sign(payload, callbackProperties.getSecret());
        vo.setDevSignatureHeader("x-gj-pay-signature");
        vo.setDevSignaturePayload(signatureSupport.canonicalPayload(payload));
        vo.setDevSignature(signature);
        vo.setDevCallbackExample("{\"payNo\":\"P202605220001\",\"thirdPayNo\":\"MOCK-P202605220001\",\"notifyId\":\"N202605220001\",\"eventType\":\"SUCCESS\",\"amount\":\"99.00\"}");
    }
}
