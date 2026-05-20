package com.gj.mall.order.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.AfterSaleApplyDTO;
import com.gj.mall.order.dto.AfterSaleReturnDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.vo.AfterSaleEligibilityVO;
import com.gj.mall.order.vo.AfterSaleVO;

import java.util.List;

public interface AfterSaleService {

    AfterSaleVO apply(Long userId, Long orderId, AfterSaleApplyDTO dto);

    AfterSaleEligibilityVO eligibility(Long userId, Long orderId, Integer type);

    List<AfterSaleVO> listByOrder(Long userId, Long orderId);

    PageResult<AfterSaleVO> page(Long userId, OrderQueryDTO query);

    void cancel(Long userId, Long id);

    void submitReturn(Long userId, Long id, AfterSaleReturnDTO dto);
}
