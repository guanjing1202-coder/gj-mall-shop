package com.gj.mall.user.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.user.vo.UserMessageVO;

public interface UserMessageService {

    PageResult<UserMessageVO> page(Long userId, Long current, Long size, Integer readStatus);

    Long unreadCount(Long userId);

    Long create(Long userId, String type, String title, String content, String bizType, Long bizId, String bizNo);

    void markRead(Long userId, Long id);

    void markAllRead(Long userId);

    void delete(Long userId, Long id);
}
