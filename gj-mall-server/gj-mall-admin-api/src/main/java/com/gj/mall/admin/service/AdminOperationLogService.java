package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminOperationLogQueryDTO;
import com.gj.mall.admin.entity.SysOperationLog;
import com.gj.mall.admin.vo.AdminOperationLogVO;
import com.gj.mall.common.result.PageResult;

public interface AdminOperationLogService {

    PageResult<AdminOperationLogVO> page(AdminOperationLogQueryDTO query);

    void record(SysOperationLog log);
}
