package com.gj.mall.order.service.impl;

import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.order.dto.FreightQuoteDTO;
import com.gj.mall.order.service.FreightService;
import com.gj.mall.order.vo.FreightQuoteVO;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FreightServiceImpl implements FreightService {

    private static final BigDecimal DEFAULT_BASE_FREIGHT = new BigDecimal("12.00");
    private static final BigDecimal DEFAULT_FREE_THRESHOLD = new BigDecimal("99.00");
    private static final BigDecimal DEFAULT_REMOTE_EXTRA = new BigDecimal("18.00");
    private static final List<String> DEFAULT_REMOTE_PROVINCES = Arrays.asList(
            "新疆维吾尔自治区", "西藏自治区", "青海省", "宁夏回族自治区", "内蒙古自治区", "海南省"
    );

    private final UserAddressService addressService;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public FreightQuoteVO quote(Long userId, FreightQuoteDTO dto) {
        if (dto == null || dto.getAddressId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "收货地址不能为空");
        }
        UmsUserAddress address = addressService.getOne(userId, dto.getAddressId());
        return calculate(dto.getOrderAmount(), address);
    }

    @Override
    public FreightQuoteVO calculate(BigDecimal orderAmount, UmsUserAddress address) {
        BigDecimal amount = money(orderAmount);
        if (amount.signum() < 0) {
            throw new BizException(ResultCode.PARAM_ERROR, "商品金额不能小于 0");
        }
        BigDecimal baseFreight = money(decimalConfig("order.freight.base", DEFAULT_BASE_FREIGHT));
        BigDecimal freeThreshold = money(decimalConfig("order.freight.free.threshold", DEFAULT_FREE_THRESHOLD));
        BigDecimal remoteExtra = money(decimalConfig("order.freight.remote.extra", DEFAULT_REMOTE_EXTRA));
        Set<String> remoteProvinces = remoteProvinces();
        String province = address == null ? null : StrUtil.trim(address.getProvince());
        boolean remoteArea = isRemoteProvince(province, remoteProvinces);
        boolean thresholdReached = freeThreshold.signum() > 0 && amount.compareTo(freeThreshold) >= 0;

        BigDecimal chargedBase = thresholdReached ? BigDecimal.ZERO : baseFreight;
        BigDecimal chargedRemote = remoteArea ? remoteExtra : BigDecimal.ZERO;
        BigDecimal freight = money(chargedBase.add(chargedRemote));

        FreightQuoteVO vo = new FreightQuoteVO();
        vo.setOrderAmount(amount);
        vo.setFreightAmount(freight);
        vo.setBaseFreight(baseFreight);
        vo.setChargedBaseFreight(money(chargedBase));
        vo.setRemoteExtra(remoteExtra);
        vo.setChargedRemoteExtra(money(chargedRemote));
        vo.setFreeThreshold(freeThreshold);
        vo.setFreeThresholdReached(thresholdReached);
        vo.setFreeShipping(freight.signum() == 0);
        vo.setRemoteArea(remoteArea);
        vo.setProvince(province);
        vo.setNextFreeAmount(thresholdReached || freeThreshold.signum() <= 0
                ? BigDecimal.ZERO
                : money(freeThreshold.subtract(amount)));
        vo.setSummary(summary(vo));
        vo.setHint(hint(vo));
        return vo;
    }

    private String summary(FreightQuoteVO vo) {
        if (Boolean.TRUE.equals(vo.getFreeShipping())) {
            return "已满足免运费条件";
        }
        if (Boolean.TRUE.equals(vo.getFreeThresholdReached()) && Boolean.TRUE.equals(vo.getRemoteArea())) {
            return "已免基础运费，偏远地区加收 " + format(vo.getChargedRemoteExtra());
        }
        if (vo.getNextFreeAmount() != null && vo.getNextFreeAmount().signum() > 0) {
            return "再买 " + format(vo.getNextFreeAmount()) + " 可免基础运费";
        }
        return "本单运费 " + format(vo.getFreightAmount());
    }

    private String hint(FreightQuoteVO vo) {
        if (Boolean.TRUE.equals(vo.getRemoteArea())) {
            if (Boolean.TRUE.equals(vo.getFreeThresholdReached())) {
                return "收货地 " + StrUtil.blankToDefault(vo.getProvince(), "当前省份") + " 属于偏远地区，免基础运费后仍保留地区附加费。";
            }
            return "收货地 " + StrUtil.blankToDefault(vo.getProvince(), "当前省份") + " 属于偏远地区，包含基础运费和地区附加费。";
        }
        if (Boolean.TRUE.equals(vo.getFreeShipping())) {
            return "商品金额已达到免基础运费门槛。";
        }
        return "商品金额满 " + format(vo.getFreeThreshold()) + " 可免基础运费。";
    }

    private BigDecimal decimalConfig(String key, BigDecimal fallback) {
        String value = configValue(key);
        if (StrUtil.isBlank(value)) {
            return fallback;
        }
        try {
            BigDecimal parsed = new BigDecimal(StrUtil.trim(value));
            return parsed.signum() < 0 ? fallback : parsed;
        } catch (NumberFormatException ex) {
            log.warn("[freight] invalid number config {}={}", key, value);
            return fallback;
        }
    }

    private Set<String> remoteProvinces() {
        String value = configValue("order.freight.remote.provinces");
        List<String> provinces = DEFAULT_REMOTE_PROVINCES;
        if (StrUtil.isNotBlank(value)) {
            try {
                provinces = JSON.parseArray(value, String.class);
            } catch (Exception ex) {
                log.warn("[freight] invalid remote provinces config: {}", value);
            }
        }
        provinces = sanitizeProvinces(provinces);
        if (provinces.isEmpty()) {
            provinces = DEFAULT_REMOTE_PROVINCES;
        }
        return provinces.stream()
                .filter(StrUtil::isNotBlank)
                .map(this::normalizeProvince)
                .collect(Collectors.toCollection(HashSet::new));
    }

    private List<String> sanitizeProvinces(List<String> provinces) {
        if (provinces == null) {
            return java.util.Collections.emptyList();
        }
        return provinces.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .filter(this::containsCjk)
                .collect(Collectors.toList());
    }

    private boolean containsCjk(String text) {
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= '\u4e00' && ch <= '\u9fff') {
                return true;
            }
        }
        return false;
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
            log.warn("[freight] read config failed key={}", key, ex);
            return null;
        }
    }

    private boolean isRemoteProvince(String province, Set<String> remoteProvinces) {
        if (StrUtil.isBlank(province) || remoteProvinces == null || remoteProvinces.isEmpty()) {
            return false;
        }
        return remoteProvinces.contains(normalizeProvince(province));
    }

    private String normalizeProvince(String province) {
        if (StrUtil.isBlank(province)) {
            return "";
        }
        return StrUtil.trim(province)
                .replace("维吾尔", "")
                .replace("回族", "")
                .replace("壮族", "")
                .replace("自治区", "")
                .replace("特别行政区", "")
                .replace("省", "")
                .replace("市", "");
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private String format(BigDecimal value) {
        BigDecimal money = money(value).stripTrailingZeros();
        return "¥" + money.toPlainString();
    }
}
