package com.gj.mall.product.service;

import com.gj.mall.product.dto.CategoryDTO;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.vo.CategoryTreeVO;

import java.util.List;

public interface CategoryService {

    /** 全量树（含隐藏） */
    List<CategoryTreeVO> tree();

    /** 仅展示的树（前台 C 端用） */
    List<CategoryTreeVO> visibleTree();

    List<PmsCategory> listByParent(Long parentId);

    Long save(CategoryDTO dto);

    void update(CategoryDTO dto);

    void delete(Long id);
}
