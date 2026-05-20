package com.gj.mall.order.service;

import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;

public interface AfterSaleRuleService {

    int windowDays();

    AfterSaleEligibilityVO evaluate(OmsOrder order, Integer requestedType);

    void validateApply(OmsOrder order, Integer requestedType);
}
