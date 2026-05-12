package com.gj.mall.user.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.user.vo.UserBrowseHistoryVO;

public interface UserBrowseHistoryService {

    PageResult<UserBrowseHistoryVO> page(Long userId, Long current, Long size);

    void record(Long userId, Long spuId);

    void remove(Long userId, Long spuId);

    void clear(Long userId);
}
