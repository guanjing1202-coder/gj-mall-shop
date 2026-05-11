package com.gj.mall.product.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.AdminSpuOperationDTO;
import com.gj.mall.product.dto.SpuQueryDTO;
import com.gj.mall.product.dto.SpuSaveDTO;
import com.gj.mall.product.vo.SpuDetailVO;
import com.gj.mall.product.vo.SpuListVO;

public interface SpuService {

    /** 前台分页（仅上架） */
    PageResult<SpuListVO> pagePublic(SpuQueryDTO query);

    /** 后台分页（全部） */
    PageResult<SpuListVO> pageAdmin(SpuQueryDTO query);

    /** 详情（含 SKU + Mongo 富文本） */
    SpuDetailVO detail(Long id);

    Long create(SpuSaveDTO dto);

    void update(SpuSaveDTO dto);

    /** 上下架 */
    void publish(Long id, Integer status);

    /** 商品运营位设置 */
    void updateOperation(Long id, AdminSpuOperationDTO dto);

    void delete(Long id);
}
