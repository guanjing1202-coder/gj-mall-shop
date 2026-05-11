package com.gj.mall.product.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.dto.BrandDTO;
import com.gj.mall.product.entity.PmsBrand;

import java.util.List;

public interface BrandService {

    PageResult<PmsBrand> page(String keyword, Long pageNum, Long pageSize);

    List<PmsBrand> listAll();

    Long save(BrandDTO dto);

    void update(BrandDTO dto);

    void delete(Long id);
}
