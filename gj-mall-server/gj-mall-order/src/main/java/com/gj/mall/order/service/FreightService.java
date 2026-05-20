package com.gj.mall.order.service;

import com.gj.mall.order.dto.FreightQuoteDTO;
import com.gj.mall.order.vo.FreightQuoteVO;
import com.gj.mall.user.entity.UmsUserAddress;

import java.math.BigDecimal;

public interface FreightService {

    FreightQuoteVO quote(Long userId, FreightQuoteDTO dto);

    FreightQuoteVO calculate(BigDecimal orderAmount, UmsUserAddress address);
}
