package com.gj.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.user.entity.UmsUserMessage;
import com.gj.mall.user.mapper.UmsUserMessageMapper;
import com.gj.mall.user.vo.UserMessageSummaryVO;
import com.gj.mall.user.vo.UserMessageVO;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserMessageServiceImplTest {

    @Test
    void summaryCountsReadUnreadAndTypes() {
        UmsUserMessageMapper messageMapper = mock(UmsUserMessageMapper.class);
        UserMessageServiceImpl service = new UserMessageServiceImpl(messageMapper);

        when(messageMapper.selectList(any(Wrapper.class))).thenReturn(Arrays.asList(
                message("order", 0),
                message("order", 1),
                message("logistics", 0),
                message("after_sale", 1),
                message(null, 0)
        ));

        UserMessageSummaryVO summary = service.summary(1001L);

        assertEquals(5L, summary.getTotal());
        assertEquals(3L, summary.getUnreadTotal());
        assertEquals(2L, summary.getReadTotal());
        assertEquals(2L, summary.getTypeItems().get(0).getTotal());
        assertEquals("订单通知", summary.getTypeItems().get(0).getTypeDesc());
        assertEquals(1L, summary.getTypeItems().get(0).getUnreadTotal());
        assertEquals("系统通知", summary.getTypeItems().get(4).getTypeDesc());
        assertEquals(1L, summary.getTypeItems().get(4).getUnreadTotal());
    }

    @Test
    void summaryReturnsStableTypesWhenEmpty() {
        UmsUserMessageMapper messageMapper = mock(UmsUserMessageMapper.class);
        UserMessageServiceImpl service = new UserMessageServiceImpl(messageMapper);

        when(messageMapper.selectList(any(Wrapper.class))).thenReturn(Collections.emptyList());

        UserMessageSummaryVO summary = service.summary(1001L);

        assertEquals(0L, summary.getTotal());
        assertEquals(0L, summary.getUnreadTotal());
        assertEquals(0L, summary.getReadTotal());
        assertEquals(5, summary.getTypeItems().size());
        assertEquals("order", summary.getTypeItems().get(0).getType());
        assertEquals("system", summary.getTypeItems().get(4).getType());
    }

    @Test
    void pageAcceptsTypeFilter() {
        UmsUserMessageMapper messageMapper = mock(UmsUserMessageMapper.class);
        UserMessageServiceImpl service = new UserMessageServiceImpl(messageMapper);
        Page<UmsUserMessage> result = new Page<>(1, 10);
        result.setTotal(1);
        result.setRecords(Collections.singletonList(message("after_sale", 0)));
        when(messageMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(result);

        UserMessageVO vo = service.page(1001L, 1L, 10L, 0, "after_sale").getList().get(0);

        assertEquals("售后通知", vo.getTypeDesc());
        verify(messageMapper).selectPage(any(Page.class), any(Wrapper.class));
    }

    @Test
    void clearReadDeletesOnlyCurrentUsersReadMessages() {
        UmsUserMessageMapper messageMapper = mock(UmsUserMessageMapper.class);
        UserMessageServiceImpl service = new UserMessageServiceImpl(messageMapper);

        service.clearRead(1001L);

        verify(messageMapper).clearRead(eq(1001L));
    }

    private UmsUserMessage message(String type, Integer readStatus) {
        UmsUserMessage message = new UmsUserMessage();
        message.setId(1L);
        message.setType(type);
        message.setTitle("测试消息");
        message.setReadStatus(readStatus);
        return message;
    }
}
