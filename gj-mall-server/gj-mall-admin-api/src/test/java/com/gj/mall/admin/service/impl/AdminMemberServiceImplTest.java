package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.MemberQueryDTO;
import com.gj.mall.admin.vo.MemberDetailVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserAddressMapper;
import com.gj.mall.user.mapper.UmsUserMapper;
import com.gj.mall.user.service.UserBrowseHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminMemberServiceImplTest {

    private UmsUserMapper userMapper;
    private UmsUserAddressMapper addressMapper;
    private OmsOrderMapper orderMapper;
    private UserBrowseHistoryService browseHistoryService;
    private AdminMemberServiceImpl service;

    @BeforeEach
    void setUp() {
        userMapper = mock(UmsUserMapper.class);
        addressMapper = mock(UmsUserAddressMapper.class);
        orderMapper = mock(OmsOrderMapper.class);
        browseHistoryService = mock(UserBrowseHistoryService.class);
        service = new AdminMemberServiceImpl(userMapper, addressMapper, orderMapper, browseHistoryService);
    }

    @Test
    void pageUsesDefaultQueryWhenQueryIsNull() {
        Page<UmsUser> mapperPage = new Page<>(1, 10);
        mapperPage.setTotal(0);
        mapperPage.setRecords(Collections.emptyList());
        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(mapperPage);

        PageResult<MemberDetailVO> result = service.page(null);

        assertNotNull(result);
        assertEquals(0L, result.getTotal());
        assertEquals(1L, result.getPageNum());
        assertEquals(10L, result.getPageSize());
        verify(userMapper).selectPage(argThat(page ->
                page.getCurrent() == 1L && page.getSize() == 10L), any(Wrapper.class));
    }

    @Test
    void pageCapsPageSizeAndCountsRefundingAndRefundedOrdersAsPaidAmount() {
        MemberQueryDTO query = new MemberQueryDTO();
        query.setPageNum(-1L);
        query.setPageSize(500L);

        Page<UmsUser> mapperPage = new Page<>(1, 100);
        mapperPage.setTotal(1);
        mapperPage.setRecords(Collections.singletonList(user()));
        when(userMapper.selectPage(any(Page.class), any(Wrapper.class))).thenReturn(mapperPage);
        when(addressMapper.selectMaps(any())).thenReturn(Collections.singletonList(addressCountRow()));
        when(orderMapper.selectMaps(any())).thenReturn(Collections.singletonList(orderSummaryRow()));

        AtomicReference<Wrapper<?>> orderWrapper = new AtomicReference<>();
        when(orderMapper.selectMaps(any())).thenAnswer(invocation -> {
            orderWrapper.set(invocation.getArgument(0));
            return Collections.singletonList(orderSummaryRow());
        });

        PageResult<MemberDetailVO> result = service.page(query);

        assertEquals(1L, result.getTotal());
        assertEquals(1L, result.getPageNum());
        assertEquals(100L, result.getPageSize());
        assertEquals(1, result.getList().size());
        assertEquals(new BigDecimal("200.00"), result.getList().get(0).getPaidAmount());
        String selectSql = ((AbstractWrapper<?, ?, ?>) orderWrapper.get()).getSqlSelect();
        assertTrue(selectSql.contains(String.valueOf(OrderStatus.REFUNDING.getCode())));
        assertTrue(selectSql.contains(String.valueOf(OrderStatus.REFUNDED.getCode())));
        verify(userMapper).selectPage(argThat(page ->
                page.getCurrent() == 1L && page.getSize() == 100L), any(Wrapper.class));
    }

    private UmsUser user() {
        UmsUser user = new UmsUser();
        user.setId(10L);
        user.setUsername("member");
        user.setNickname("会员");
        user.setPhone("13800000000");
        user.setStatus(1);
        user.setCreateTime(LocalDateTime.parse("2026-06-09T10:00:00"));
        return user;
    }

    private Map<String, Object> addressCountRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", 10L);
        row.put("address_count", 2L);
        return row;
    }

    private Map<String, Object> orderSummaryRow() {
        Map<String, Object> row = new HashMap<>();
        row.put("user_id", 10L);
        row.put("order_count", 3L);
        row.put("paid_amount", new BigDecimal("200.00"));
        return row;
    }

}
