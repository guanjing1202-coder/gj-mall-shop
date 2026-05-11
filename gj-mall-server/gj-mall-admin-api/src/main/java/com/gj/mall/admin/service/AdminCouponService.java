package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminCouponIssueDTO;
import com.gj.mall.admin.dto.AdminCouponQueryDTO;
import com.gj.mall.admin.dto.AdminCouponSaveDTO;
import com.gj.mall.admin.dto.AdminCouponUserQueryDTO;
import com.gj.mall.admin.vo.AdminCouponUserVO;
import com.gj.mall.admin.vo.AdminCouponVO;
import com.gj.mall.common.result.PageResult;

public interface AdminCouponService {

    PageResult<AdminCouponVO> page(AdminCouponQueryDTO query);

    AdminCouponVO detail(Long id);

    Long create(AdminCouponSaveDTO dto);

    void update(AdminCouponSaveDTO dto);

    void updateStatus(Long id, Integer status);

    void delete(Long id);

    Integer issue(Long couponId, AdminCouponIssueDTO dto);

    PageResult<AdminCouponUserVO> users(Long couponId, AdminCouponUserQueryDTO query);
}
