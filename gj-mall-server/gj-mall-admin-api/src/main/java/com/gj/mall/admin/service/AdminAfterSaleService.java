package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminAfterSaleActionDTO;
import com.gj.mall.admin.dto.AdminAfterSaleCreateDTO;
import com.gj.mall.admin.dto.AdminAfterSaleQueryDTO;
import com.gj.mall.admin.vo.AdminAfterSaleSummaryVO;
import com.gj.mall.admin.vo.AdminAfterSaleVO;
import com.gj.mall.common.result.PageResult;

public interface AdminAfterSaleService {

    AdminAfterSaleSummaryVO summary();

    PageResult<AdminAfterSaleVO> page(AdminAfterSaleQueryDTO query);

    AdminAfterSaleVO detail(Long id);

    Long create(AdminAfterSaleCreateDTO dto);

    void approve(Long id, AdminAfterSaleActionDTO dto);

    void reject(Long id, AdminAfterSaleActionDTO dto);

    void receive(Long id, AdminAfterSaleActionDTO dto);

    void refund(Long id, AdminAfterSaleActionDTO dto);

    void cancel(Long id, AdminAfterSaleActionDTO dto);
}
