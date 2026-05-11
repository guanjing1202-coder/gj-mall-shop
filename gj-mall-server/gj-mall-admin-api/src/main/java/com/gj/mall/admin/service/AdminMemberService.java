package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.MemberQueryDTO;
import com.gj.mall.admin.vo.MemberDetailVO;
import com.gj.mall.common.result.PageResult;

public interface AdminMemberService {

    PageResult<MemberDetailVO> page(MemberQueryDTO query);

    MemberDetailVO detail(Long id);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
