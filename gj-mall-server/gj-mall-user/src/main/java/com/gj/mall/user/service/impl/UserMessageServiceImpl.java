package com.gj.mall.user.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.user.entity.UmsUserMessage;
import com.gj.mall.user.mapper.UmsUserMessageMapper;
import com.gj.mall.user.service.UserMessageService;
import com.gj.mall.user.vo.UserMessageSummaryVO;
import com.gj.mall.user.vo.UserMessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserMessageServiceImpl implements UserMessageService {

    private final UmsUserMessageMapper messageMapper;

    @Override
    public PageResult<UserMessageVO> page(Long userId, Long current, Long size, Integer readStatus, String type) {
        long pageNum = normalizePageNum(current);
        long pageSize = normalizePageSize(size);
        IPage<UmsUserMessage> result = messageMapper.selectPage(
                new Page<>(pageNum, pageSize),
                Wrappers.<UmsUserMessage>lambdaQuery()
                        .eq(UmsUserMessage::getUserId, userId)
                        .eq(readStatus != null, UmsUserMessage::getReadStatus, readStatus)
                        .eq(StrUtil.isNotBlank(type), UmsUserMessage::getType, normalizeType(type))
                        .orderByAsc(UmsUserMessage::getReadStatus)
                        .orderByDesc(UmsUserMessage::getCreateTime));
        if (result.getRecords() == null || result.getRecords().isEmpty()) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(UserMessageVO::from).collect(Collectors.toList()));
    }

    @Override
    public Long unreadCount(Long userId) {
        return messageMapper.selectCount(Wrappers.<UmsUserMessage>lambdaQuery()
                .eq(UmsUserMessage::getUserId, userId)
                .eq(UmsUserMessage::getReadStatus, 0));
    }

    @Override
    public UserMessageSummaryVO summary(Long userId) {
        List<UmsUserMessage> messages = messageMapper.selectList(new QueryWrapper<UmsUserMessage>()
                .select("type", "read_status")
                .eq("user_id", userId)
                .eq("deleted", 0));

        Map<String, UserMessageSummaryVO.TypeItem> typeMap = defaultTypeMap();
        long total = 0L;
        long unreadTotal = 0L;
        for (UmsUserMessage message : messages) {
            total++;
            String type = normalizeType(message.getType());
            UserMessageSummaryVO.TypeItem item = typeMap.computeIfAbsent(
                    type,
                    key -> UserMessageSummaryVO.TypeItem.empty(key, UserMessageVO.typeDesc(key)));
            item.setTotal(item.getTotal() + 1);
            if (Integer.valueOf(0).equals(message.getReadStatus())) {
                unreadTotal++;
                item.setUnreadTotal(item.getUnreadTotal() + 1);
            } else {
                item.setReadTotal(item.getReadTotal() + 1);
            }
        }

        UserMessageSummaryVO summary = new UserMessageSummaryVO();
        summary.setTotal(total);
        summary.setUnreadTotal(unreadTotal);
        summary.setReadTotal(Math.max(0L, total - unreadTotal));
        summary.setTypeItems(typeMap.values().stream().collect(Collectors.toList()));
        return summary;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long userId, String type, String title, String content, String bizType, Long bizId, String bizNo) {
        if (userId == null || StrUtil.isBlank(title)) {
            return null;
        }
        UmsUserMessage message = new UmsUserMessage();
        message.setUserId(userId);
        message.setType(StrUtil.sub(StrUtil.blankToDefault(type, "system"), 0, 32));
        message.setTitle(StrUtil.sub(StrUtil.trim(title), 0, 100));
        message.setContent(StrUtil.sub(StrUtil.blankToDefault(StrUtil.trim(content), title), 0, 1000));
        message.setBizType(StrUtil.isBlank(bizType) ? null : StrUtil.sub(StrUtil.trim(bizType), 0, 32));
        message.setBizId(bizId);
        message.setBizNo(StrUtil.isBlank(bizNo) ? null : StrUtil.sub(StrUtil.trim(bizNo), 0, 64));
        message.setReadStatus(0);
        messageMapper.insert(message);
        return message.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markRead(Long userId, Long id) {
        UmsUserMessage message = mustOwn(userId, id);
        if (Integer.valueOf(1).equals(message.getReadStatus())) {
            return;
        }
        UmsUserMessage update = new UmsUserMessage();
        update.setId(id);
        update.setReadStatus(1);
        update.setReadTime(LocalDateTime.now());
        messageMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markAllRead(Long userId) {
        messageMapper.markAllRead(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long userId, Long id) {
        mustOwn(userId, id);
        messageMapper.deleteById(id);
    }

    private UmsUserMessage mustOwn(Long userId, Long id) {
        if (id == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "消息ID不能为空");
        }
        UmsUserMessage message = messageMapper.selectById(id);
        if (message == null || !userId.equals(message.getUserId())) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "消息不存在");
        }
        return message;
    }

    private long normalizePageNum(Long pageNum) {
        return pageNum == null || pageNum <= 0 ? 1 : pageNum;
    }

    private long normalizePageSize(Long pageSize) {
        if (pageSize == null || pageSize <= 0) {
            return 10;
        }
        return Math.min(pageSize, 50);
    }

    private Map<String, UserMessageSummaryVO.TypeItem> defaultTypeMap() {
        Map<String, UserMessageSummaryVO.TypeItem> map = new LinkedHashMap<>();
        addType(map, "order");
        addType(map, "logistics");
        addType(map, "payment");
        addType(map, "after_sale");
        addType(map, "system");
        return map;
    }

    private void addType(Map<String, UserMessageSummaryVO.TypeItem> map, String type) {
        map.put(type, UserMessageSummaryVO.TypeItem.empty(type, UserMessageVO.typeDesc(type)));
    }

    private String normalizeType(String type) {
        return StrUtil.blankToDefault(type, "system");
    }
}
