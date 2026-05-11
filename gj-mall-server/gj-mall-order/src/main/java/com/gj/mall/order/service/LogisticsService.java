package com.gj.mall.order.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.AdminDeliveryCompanyQueryDTO;
import com.gj.mall.order.dto.AdminDeliveryCompanySaveDTO;
import com.gj.mall.order.dto.AdminLogisticsOrderQueryDTO;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.vo.AdminDeliveryCompanyVO;
import com.gj.mall.order.vo.AdminLogisticsSummaryVO;
import com.gj.mall.order.vo.OrderVO;

import java.util.List;

public interface LogisticsService {

    AdminLogisticsSummaryVO summary();

    PageResult<OrderVO> orderPage(AdminLogisticsOrderQueryDTO query);

    OrderVO deliver(Long orderId, AdminOrderDeliverDTO dto);

    PageResult<AdminDeliveryCompanyVO> companyPage(AdminDeliveryCompanyQueryDTO query);

    List<AdminDeliveryCompanyVO> enabledCompanies();

    Long createCompany(AdminDeliveryCompanySaveDTO dto);

    void updateCompany(AdminDeliveryCompanySaveDTO dto);

    void updateCompanyStatus(Long id, Integer status);

    void deleteCompany(Long id);
}
