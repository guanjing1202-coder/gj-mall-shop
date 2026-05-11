package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminPaymentQueryDTO;
import com.gj.mall.admin.dto.AdminPaymentRefundDTO;
import com.gj.mall.admin.vo.AdminPaymentVO;
import com.gj.mall.common.result.PageResult;

public interface AdminPaymentService {

    PageResult<AdminPaymentVO> page(AdminPaymentQueryDTO query);

    AdminPaymentVO detail(Long id);

    void markPaid(Long id, String thirdPayNo);

    void markFailed(Long id, String reason);

    void refund(Long id, AdminPaymentRefundDTO dto);
}
