package com.gj.mall.pay.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.PayPaymentRecord;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.enums.PayChannel;
import com.gj.mall.order.mapper.PayPaymentRecordMapper;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.pay.dto.PayDTO;
import com.gj.mall.pay.service.PayService;
import com.gj.mall.pay.strategy.PayStrategy;
import com.gj.mall.pay.vo.PayResultVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PayServiceImpl implements PayService {

    private final List<PayStrategy> strategies;
    private final OrderService orderService;
    private final PayPaymentRecordMapper recordMapper;

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
}
