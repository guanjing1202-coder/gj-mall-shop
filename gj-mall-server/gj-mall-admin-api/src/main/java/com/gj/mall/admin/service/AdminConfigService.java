package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminConfigQueryDTO;
import com.gj.mall.admin.dto.AdminConfigSaveDTO;
import com.gj.mall.admin.vo.AdminConfigVO;
import com.gj.mall.common.result.PageResult;

public interface AdminConfigService {

    PageResult<AdminConfigVO> page(AdminConfigQueryDTO query);

    AdminConfigVO detail(Long id);

    Long create(AdminConfigSaveDTO dto);

    void update(AdminConfigSaveDTO dto);

    void updateStatus(Long id, Integer status);

    void delete(Long id);
}
