package com.gj.mall.product.service;

import com.gj.mall.product.vo.SearchHotWordVO;
import com.gj.mall.product.vo.SearchSuggestVO;

import java.util.List;

public interface ProductSearchService {

    List<SearchHotWordVO> hotWords(Integer limit);

    List<SearchSuggestVO> suggest(String keyword, Integer limit);
}
