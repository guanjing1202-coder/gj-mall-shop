package com.gj.mall.product.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.product.vo.ProductCommentSummaryVO;
import com.gj.mall.product.vo.ProductCommentVO;

public interface ProductCommentService {

    PageResult<ProductCommentVO> page(Long spuId, Long current, Long size, Boolean hasImage);

    ProductCommentSummaryVO summary(Long spuId);
}
