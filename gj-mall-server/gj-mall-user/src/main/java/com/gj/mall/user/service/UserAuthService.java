package com.gj.mall.user.service;

import com.gj.mall.user.dto.LoginDTO;
import com.gj.mall.user.dto.RegisterDTO;
import com.gj.mall.user.vo.LoginVO;

public interface UserAuthService {

    LoginVO register(RegisterDTO dto, String ip);

    LoginVO login(LoginDTO dto, String ip);

    LoginVO refresh(String refreshToken);

    void logout(Long userId);
}
