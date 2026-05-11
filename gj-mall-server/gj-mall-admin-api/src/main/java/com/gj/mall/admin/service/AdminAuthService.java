package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminLoginDTO;
import com.gj.mall.admin.vo.AdminLoginVO;

public interface AdminAuthService {

    AdminLoginVO login(AdminLoginDTO dto);

    AdminLoginVO refresh(String refreshToken);

    void logout(Long adminId);
}
