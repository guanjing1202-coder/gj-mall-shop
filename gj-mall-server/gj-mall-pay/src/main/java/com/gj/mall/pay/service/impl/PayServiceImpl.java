package com.gj.mall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayCallbackRecord;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.order.mapper.PayCallbackRecordMapper;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.config.PayCallbackProperties;
import com.gj.mall.pay.config.PayRuntimeConfigService;
import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.strategy.PayStrategy;
import com.gj.mall.pay.support.PayCallbackEventSupport;
import com.gj.mall.pay.support.PayCallbackSignatureSupport;
import com.gj.mall.pay.vo.PayCallbackResultVO;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final List<PayStrategy> strategies;
    private final OrderService orderService;
    private final PayPaymentRecordMapper recordMapper;
    private final PayCallbackRecordMapper callbackRecordMapper;
    private final PayCallbackProperties callbackProperties;
    private final PayRuntimeConfigService runtimeConfigService;
    private final PayCallbackSignatureSupport signatureSupport;
    private final PayCallbackEventSupport eventSupport;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    private final Map<String, PayStrategy> strategyMap = new HashMap<>();

    @PostConstruct
    public void initMap() {
        for (PayStrategy s : strategies) {
            strategyMap.put(s.channelName().toLowerCase(), s);
        }
        log.info("[pay] available channels: {}", strategyMap.keySet());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PayResultVO pay(Long userId, PayDTO dto) {
        OmsOrder order = orderService.getByIdOrThrow(dto.getOrderId());
        if (!order.getUserId().equals(userId)) {
            throw new BizException(ResultCode.ORDER_NOT_FOUND);
        }
        if (OrderStatus.CANCELED.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_CANCELED);
        }
        if (!OrderStatus.PENDING_PAY.getCode().equals(order.getStatus())) {
            throw new BizException(ResultCode.ORDER_PAID);
        }

        PayStrategy strategy = strategyMap.get(dto.getChannel().toLowerCase());
        if (strategy == null) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "未知渠道：" + dto.getChannel());
        }

        // 1. 落库支付流水（待支付）
        String payNo = genPayNo(order.getOrderNo());
        PayPaymentRecord record = new PayPaymentRecord();
        record.setOrderId(order.getId());
        record.setOrderNo(order.getOrderNo());
        record.setUserId(userId);
        record.setPayNo(payNo);
        record.setChannel(strategy.channelCode());
        record.setAmount(order.getPayAmount());
        record.setStatus(0);
        recordMapper.insert(record);

        // 2. 调起渠道支付
        PayResultVO result = strategy.pay(order, payNo);
        result.setChannel(strategy.channelName());

        // 3. mock：当场完成支付并推进订单
        if (Boolean.TRUE.equals(result.getPaid())) {
            doMarkPaid(record, strategy.channelCode(), result.getThirdPayNo(), "mock-immediate");
            orderService.markPaid(order.getId(), strategy.channelCode());
            log.info("[pay] mock paid: orderNo={} payNo={}", order.getOrderNo(), payNo);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyPaid(String payNo, String thirdPayNo, String rawCallback) {
        PayPaymentRecord record = recordMapper.selectOne(
                Wrappers.<PayPaymentRecord>lambdaQuery().eq(PayPaymentRecord::getPayNo, payNo));
        if (record == null) throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND);
        if (Integer.valueOf(1).equals(record.getStatus())) {
            // 幂等：已成功，直接返回
            return;
        }
        doMarkPaid(record, record.getChannel(), thirdPayNo, rawCallback);
        orderService.markPaid(record.getOrderId(), record.getChannel());
    }

    @Override
    public PayCallbackResultVO handleCallback(String channel, Map<String, Object> payload, Map<String, String> headers) {
        try {
            return callbackTransactionTemplate().execute(status -> processCallback(channel, payload, headers, null));
        } catch (RuntimeException ex) {
            throw ex;
        }
    }

    @Override
    public PayCallbackResultVO replayCallback(Long callbackId) {
        PayCallbackRecord source = callbackRecordMapper.selectById(callbackId);
        if (source == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "回调记录不存在");
        }
        if (!Integer.valueOf(3).equals(source.getProcessStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "仅处理失败的回调可重放");
        }
        if (Integer.valueOf(2).equals(source.getSignatureStatus())) {
            throw new BizException(ResultCode.OPERATION_FORBIDDEN, "验签失败回调不可重放");
        }
        Map<String, Object> payload = parseObjectMap(source.getRawData());
        Map<String, String> headers = parseStringMap(source.getRequestHeaders());
        String channel = firstNonBlank(source.getChannelName(), Optional.ofNullable(PayChannel.of(source.getChannel()))
                .map(PayChannel::getName)
                .orElse(null));
        PayCallbackResultVO result;
        try {
            result = callbackTransactionTemplate().execute(status -> processCallback(channel, payload, headers, source));
        } catch (RuntimeException ex) {
            updateReplaySource(source, 3, source.getErrorMessage(), "重放失败：" + ex.getMessage());
            throw ex;
        }
        updateReplaySource(source, 1, source.getErrorMessage(), "重放成功：" + result.getCallbackNo());
        return result;
    }

    private PayCallbackResultVO processCallback(String channel, Map<String, Object> payload, Map<String, String> headers, PayCallbackRecord replaySource) {
        PayChannel payChannel = PayChannel.ofName(channel);
        if (payChannel == null) {
            throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "未知回调渠道：" + channel);
        }
        Map<String, Object> safePayload = payload == null ? Collections.emptyMap() : new LinkedHashMap<>(payload);
        Map<String, String> safeHeaders = headers == null ? Collections.emptyMap() : new LinkedHashMap<>(headers);
        String payNo = firstText(safePayload, "payNo", "out_trade_no", "outTradeNo");
        String thirdPayNo = firstText(safePayload, "thirdPayNo", "transaction_id", "trade_no");
        String notifyId = firstText(safePayload, "notifyId", "notify_id", "eventId", "id");
        String eventType = firstText(safePayload, "eventType", "event_type", "trade_status", "tradeState");
        BigDecimal amount = firstAmount(safePayload, "amount", "total_amount", "totalAmount", "payer_total");
        String rawData = toJson(safePayload);

        PayCallbackRecord callbackRecord = new PayCallbackRecord();
        callbackRecord.setCallbackNo(genCallbackNo(payChannel.getName()));
        callbackRecord.setChannel(payChannel.getCode());
        callbackRecord.setChannelName(payChannel.getName());
        callbackRecord.setPayNo(payNo);
        callbackRecord.setThirdPayNo(thirdPayNo);
        callbackRecord.setNotifyId(notifyId);
        callbackRecord.setEventType(eventType);
        callbackRecord.setAmount(amount);
        callbackRecord.setRetryCount(0);
        callbackRecord.setRawData(rawData);
        callbackRecord.setRequestHeaders(toJson(safeHeaders));

        int signatureStatus = replaySource == null
                ? signatureSupport.verify(safePayload, safeHeaders, runtimeCallbackProperties()).status()
                : replaySource.getSignatureStatus();
        callbackRecord.setSignatureStatus(signatureStatus);
        if (signatureStatus == 2) {
            callbackRecord.setProcessStatus(3);
            callbackRecord.setErrorMessage("签名校验失败");
            saveCallbackRecord(callbackRecord);
            throw new BizException(ResultCode.PAY_FAIL, "支付回调签名校验失败");
        }

        PayCallbackResultVO result = new PayCallbackResultVO();
        result.setCallbackNo(callbackRecord.getCallbackNo());
        result.setPayNo(payNo);
        result.setThirdPayNo(thirdPayNo);
        result.setChannel(payChannel.getName());

        try {
            PayPaymentRecord record = loadRecord(payNo, thirdPayNo, notifyId);
            callbackRecord.setPayNo(record.getPayNo());
            callbackRecord.setThirdPayNo(firstNonBlank(thirdPayNo, record.getThirdPayNo()));
            callbackRecord.setAmount(amount == null ? record.getAmount() : amount);
            if (amount != null && record.getAmount() != null && amount.compareTo(record.getAmount()) != 0) {
                throw new BizException(ResultCode.PAY_AMOUNT_MISMATCH,
                        "回调金额 " + amount + " 与支付流水金额 " + record.getAmount() + " 不一致");
            }
            if (!payChannel.getCode().equals(record.getChannel())) {
                throw new BizException(ResultCode.PAY_CHANNEL_NOT_SUPPORT, "回调渠道与支付流水渠道不一致");
            }
            if (Integer.valueOf(1).equals(record.getStatus())) {
                callbackRecord.setProcessStatus(2);
                result.setProcessed(false);
                result.setDuplicate(true);
                result.setMessage("支付流水已处理，重复回调已忽略");
            } else if (Integer.valueOf(3).equals(record.getStatus())) {
                callbackRecord.setProcessStatus(2);
                result.setProcessed(false);
                result.setDuplicate(true);
                result.setMessage("支付流水已退款，回调已忽略");
            } else if (!eventSupport.isPaidEvent(payChannel, eventType)) {
                callbackRecord.setProcessStatus(2);
                result.setProcessed(false);
                result.setDuplicate(false);
                result.setMessage("非支付成功事件，回调已记录但未入账");
            } else {
                doMarkPaid(record, record.getChannel(), callbackRecord.getThirdPayNo(), appendCallback(record.getCallbackData(),
                        "channel-callback callbackNo=" + callbackRecord.getCallbackNo()
                                + " eventType=" + firstNonBlank(eventType, "-")
                                + " notifyId=" + firstNonBlank(notifyId, "-")));
                orderService.markPaid(record.getOrderId(), record.getChannel());
                callbackRecord.setProcessStatus(1);
                result.setProcessed(true);
                result.setDuplicate(false);
                result.setMessage("支付回调已处理");
            }
            callbackRecordMapper.insert(callbackRecord);
            return result;
        } catch (RuntimeException ex) {
            callbackRecord.setProcessStatus(3);
            callbackRecord.setErrorMessage(ex.getMessage());
            saveCallbackRecord(callbackRecord);
            throw ex;
        }
    }

    private void doMarkPaid(PayPaymentRecord record, Integer channel, String thirdPayNo, String callback) {
        PayPaymentRecord upd = new PayPaymentRecord();
        upd.setId(record.getId());
        upd.setStatus(1);
        upd.setThirdPayNo(thirdPayNo);
        upd.setPayTime(LocalDateTime.now());
        upd.setCallbackData(callback);
        recordMapper.updateById(upd);
    }

    private String genPayNo(String orderNo) {
        return "P" + orderNo + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private PayPaymentRecord loadRecord(String payNo, String thirdPayNo, String notifyId) {
        PayPaymentRecord record = null;
        if (payNo != null && !payNo.trim().isEmpty()) {
            record = recordMapper.selectOne(Wrappers.<PayPaymentRecord>lambdaQuery()
                    .eq(PayPaymentRecord::getPayNo, payNo));
        }
        if (record == null && thirdPayNo != null && !thirdPayNo.trim().isEmpty()) {
            record = recordMapper.selectOne(Wrappers.<PayPaymentRecord>lambdaQuery()
                    .eq(PayPaymentRecord::getThirdPayNo, thirdPayNo));
        }
        if (record == null) {
            throw new BizException(ResultCode.PAY_RECORD_NOT_FOUND,
                    "未找到支付流水：" + firstNonBlank(payNo, thirdPayNo, notifyId, "-"));
        }
        return record;
    }

    private String firstText(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (!text.isEmpty()) {
                return text;
            }
        }
        return null;
    }

    private BigDecimal firstAmount(Map<String, Object> payload, String... keys) {
        String text = firstText(payload, keys);
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text);
        } catch (NumberFormatException ex) {
            throw new BizException(ResultCode.PAY_AMOUNT_MISMATCH, "支付回调金额格式错误");
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String appendCallback(String source, String text) {
        String line = "[" + LocalDateTime.now() + "] " + text;
        return source == null || source.trim().isEmpty() ? line : source + "\n" + line;
    }

    private String genCallbackNo(String channel) {
        return "CB" + channel.toUpperCase(Locale.ROOT) + System.currentTimeMillis()
                + String.format("%04d", ThreadLocalRandom.current().nextInt(10000));
    }

    private TransactionTemplate callbackTransactionTemplate() {
        return new TransactionTemplate(transactionManager);
    }

    private PayCallbackProperties runtimeCallbackProperties() {
        if (runtimeConfigService == null) {
            return callbackProperties;
        }
        PayCallbackProperties properties = new PayCallbackProperties();
        properties.setRequireSignature(runtimeConfigService.callbackRequireSignature());
        properties.setSecret(runtimeConfigService.callbackSecret());
        return properties;
    }

    private void saveCallbackRecord(PayCallbackRecord callbackRecord) {
        requiresNewTransactionTemplate().executeWithoutResult(status -> callbackRecordMapper.insert(callbackRecord));
    }

    private void updateReplaySource(PayCallbackRecord source, Integer processStatus, String oldMessage, String message) {
        PayCallbackRecord update = new PayCallbackRecord();
        update.setId(source.getId());
        update.setProcessStatus(processStatus);
        update.setRetryCount((source.getRetryCount() == null ? 0 : source.getRetryCount()) + 1);
        update.setErrorMessage(appendMessage(oldMessage, message));
        requiresNewTransactionTemplate().executeWithoutResult(status -> callbackRecordMapper.updateById(update));
    }

    private TransactionTemplate requiresNewTransactionTemplate() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseObjectMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Object value = objectMapper.readValue(json, Map.class);
            if (value instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) value);
            }
            return Collections.emptyMap();
        } catch (Exception ex) {
            throw new BizException(ResultCode.PARAM_ERROR, "回调原始报文解析失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseStringMap(String json) {
        if (json == null || json.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        try {
            Object value = objectMapper.readValue(json, Map.class);
            if (!(value instanceof Map)) {
                return Collections.emptyMap();
            }
            Map<String, String> result = new LinkedHashMap<>();
            ((Map<Object, Object>) value).forEach((key, item) -> {
                if (key != null && item != null) {
                    result.put(String.valueOf(key), String.valueOf(item));
                }
            });
            return result;
        } catch (Exception ex) {
            throw new BizException(ResultCode.PARAM_ERROR, "回调请求头解析失败");
        }
    }

    private String appendMessage(String source, String text) {
        return source == null || source.trim().isEmpty() ? text : source + "\n" + text;
    }
}
