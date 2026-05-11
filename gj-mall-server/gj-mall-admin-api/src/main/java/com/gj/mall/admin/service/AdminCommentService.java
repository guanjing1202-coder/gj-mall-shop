package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminCommentActionDTO;
import com.gj.mall.admin.dto.AdminCommentQueryDTO;
import com.gj.mall.admin.dto.AdminCommentReplyDTO;
import com.gj.mall.admin.vo.AdminCommentVO;
import com.gj.mall.common.result.PageResult;

public interface AdminCommentService {

    PageResult<AdminCommentVO> page(AdminCommentQueryDTO query);

    AdminCommentVO detail(Long id);

    void approve(Long id, AdminCommentActionDTO dto);

    void reject(Long id, AdminCommentActionDTO dto);

    void hide(Long id, AdminCommentActionDTO dto);

    void show(Long id, AdminCommentActionDTO dto);

    void reply(Long id, AdminCommentReplyDTO dto);

    void delete(Long id);
}
