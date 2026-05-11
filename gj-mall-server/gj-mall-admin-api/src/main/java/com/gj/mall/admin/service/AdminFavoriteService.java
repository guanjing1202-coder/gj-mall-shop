package com.gj.mall.admin.service;

import com.gj.mall.admin.dto.AdminFavoriteBatchDeleteDTO;
import com.gj.mall.admin.dto.AdminFavoriteQueryDTO;
import com.gj.mall.admin.vo.AdminFavoriteVO;
import com.gj.mall.common.result.PageResult;

public interface AdminFavoriteService {

    PageResult<AdminFavoriteVO> page(AdminFavoriteQueryDTO query);

    void delete(Long id);

    void batchDelete(AdminFavoriteBatchDeleteDTO dto);
}
