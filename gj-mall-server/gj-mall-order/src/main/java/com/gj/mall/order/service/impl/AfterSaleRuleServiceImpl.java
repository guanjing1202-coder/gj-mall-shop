package com.gj.mall.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.service.AfterSaleRuleService;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AfterSaleRuleServiceImpl implements AfterSaleRuleService {

    private static final String WINDOW_DAYS_KEY = "after.sale.window.days";

    private final JdbcTemplate jdbcTemplate;

    @Override
    public int windowDays() {
        String value = configValue(WINDOW_DAYS_KEY);
        if (StrUtil.isBlank(value)) {
            return AfterSalePolicy.DEFAULT_WINDOW_DAYS;
        }
        try {
            int days = Integer.parseInt(StrUtil.trim(value));
            return days <= 0 ? AfterSalePolicy.DEFAULT_WINDOW_DAYS : days;
        } catch (NumberFormatException ex) {
            log.warn("[after-sale] invalid window days config {}={}", WINDOW_DAYS_KEY, value);
            return AfterSalePolicy.DEFAULT_WINDOW_DAYS;
        }
    }

    @Override
    public AfterSaleEligibilityVO evaluate(OmsOrder order, Integer requestedType) {
        return AfterSalePolicy.evaluate(order, requestedType, windowDays(), LocalDateTime.now());
    }

    @Override
    public void validateApply(OmsOrder order, Integer requestedType) {
        AfterSalePolicy.validateApply(order, requestedType, windowDays(), LocalDateTime.now());
    }

    private String configValue(String key) {
        try {
            return jdbcTemplate.queryForObject(
                    "select config_value from sys_config where config_key = ? and status = 1 and deleted = 0 limit 1",
                    String.class,
                    key);
        } catch (EmptyResultDataAccessException ignored) {
            return null;
        } catch (DataAccessException ex) {
            log.warn("[after-sale] read config failed key={}", key, ex);
            return null;
        }
    }
}
