package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminSeckillQueryDTO;
import com.gj.mall.admin.dto.AdminSeckillSaveDTO;
import com.gj.mall.admin.dto.AdminSeckillSkuSaveDTO;
import com.gj.mall.admin.vo.AdminSeckillVO;
import com.gj.mall.common.result.PageResult;

public interface AdminSeckillService {

    PageResult<AdminSeckillVO> page(AdminSeckillQueryDTO query);

    AdminSeckillVO detail(Long id);

    Long create(AdminSeckillSaveDTO dto);

    void update(AdminSeckillSaveDTO dto);

    void updateStatus(Long id, Integer status);

    void delete(Long id);

    Long saveSku(AdminSeckillSkuSaveDTO dto);

    void deleteSku(Long skuId);

    void warmUp(Long id);
}
