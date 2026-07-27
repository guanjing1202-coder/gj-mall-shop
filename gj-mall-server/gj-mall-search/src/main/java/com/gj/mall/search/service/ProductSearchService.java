package com.gj.mall.search.service;

import com.gj.mall.search.vo.SearchHotWordVO;
import com.gj.mall.search.vo.SearchSuggestVO;

import java.util.List;

public interface ProductSearchService {

    List<SearchHotWordVO> hotWords(Integer limit);

    List<SearchSuggestVO> suggest(String keyword, Integer limit);
}
