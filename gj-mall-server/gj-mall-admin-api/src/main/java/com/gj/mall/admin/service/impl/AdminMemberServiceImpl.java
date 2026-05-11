package com.gj.mall.admin.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.dto.MemberQueryDTO;
import com.gj.mall.admin.service.AdminMemberService;
import com.gj.mall.admin.vo.MemberDetailVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.mapper.UmsUserAddressMapper;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminMemberServiceImpl implements AdminMemberService {

    private final UmsUserMapper userMapper;
    private final UmsUserAddressMapper addressMapper;
    private final OmsOrderMapper orderMapper;

    @Override
    public PageResult<MemberDetailVO> page(MemberQueryDTO query) {
        long pageNum = query.getPageNum() == null || query.getPageNum() <= 0 ? 1 : query.getPageNum();
        long pageSize = query.getPageSize() == null || query.getPageSize() <= 0 ? 10 : query.getPageSize();
        Page<UmsUser> page = new Page<>(pageNum, pageSize);
        IPage<UmsUser> result = userMapper.selectPage(page,
                Wrappers.<UmsUser>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(UmsUser::getUsername, query.getKeyword())
                                .or()
                                .like(UmsUser::getNickname, query.getKeyword())
                                .or()
                                .like(UmsUser::getPhone, query.getKeyword()))
                        .eq(query.getStatus() != null, UmsUser::getStatus, query.getStatus())
                        .orderByDesc(UmsUser::getCreateTime));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                enrichMembers(result.getRecords(), false));
    }

    @Override
    public MemberDetailVO detail(Long id) {
        UmsUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        return enrichMembers(Collections.singletonList(user), true).get(0);
    }

    @Override
    public void updateStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        UmsUser exists = userMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.USER_NOT_FOUND);
        }
        UmsUser update = new UmsUser();
        update.setId(id);
        update.setStatus(status);
        userMapper.updateById(update);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        userMapper.deleteById(id);
        addressMapper.delete(Wrappers.<UmsUserAddress>lambdaQuery()
                .eq(UmsUserAddress::getUserId, id));
    }

    private List<MemberDetailVO> enrichMembers(List<UmsUser> users, boolean withAddresses) {
        List<Long> userIds = users.stream().map(UmsUser::getId).collect(Collectors.toList());
        Map<Long, Long> addressCountMap = loadAddressCounts(userIds);
        Map<Long, OrderSummary> orderSummaryMap = loadOrderSummaries(userIds);
        Map<Long, List<UmsUserAddress>> addressMap = withAddresses ? loadAddresses(userIds) : Collections.emptyMap();

        return users.stream().map(user -> {
            OrderSummary orderSummary = orderSummaryMap.getOrDefault(user.getId(), OrderSummary.empty());
            return MemberDetailVO.from(
                    user,
                    addressCountMap.getOrDefault(user.getId(), 0L),
                    orderSummary.orderCount,
                    orderSummary.paidAmount,
                    addressMap.getOrDefault(user.getId(), Collections.emptyList()));
        }).collect(Collectors.toList());
    }

    private Map<Long, Long> loadAddressCounts(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        QueryWrapper<UmsUserAddress> wrapper = Wrappers.query();
        wrapper.select("user_id", "COUNT(*) AS address_count");
        wrapper.in("user_id", userIds);
        wrapper.groupBy("user_id");
        return addressMapper.selectMaps(wrapper).stream()
                .collect(Collectors.toMap(
                        row -> toLong(row.get("user_id")),
                        row -> toLong(row.get("address_count"))));
    }

    private Map<Long, List<UmsUserAddress>> loadAddresses(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return addressMapper.selectList(Wrappers.<UmsUserAddress>lambdaQuery()
                        .in(UmsUserAddress::getUserId, userIds)
                        .orderByDesc(UmsUserAddress::getIsDefault)
                        .orderByDesc(UmsUserAddress::getCreateTime))
                .stream()
                .collect(Collectors.groupingBy(UmsUserAddress::getUserId));
    }

    private Map<Long, OrderSummary> loadOrderSummaries(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        QueryWrapper<OmsOrder> wrapper = Wrappers.query();
        wrapper.select(
                "user_id",
                "COUNT(*) AS order_count",
                "COALESCE(SUM(CASE WHEN status IN (1, 2, 3) THEN pay_amount ELSE 0 END), 0) AS paid_amount");
        wrapper.in("user_id", userIds);
        wrapper.groupBy("user_id");
        List<Map<String, Object>> rows = orderMapper.selectMaps(wrapper);
        Map<Long, OrderSummary> map = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long userId = toLong(row.get("user_id"));
            Long orderCount = toLong(row.get("order_count"));
            BigDecimal paidAmount = toBigDecimal(row.get("paid_amount"));
            map.put(userId, new OrderSummary(orderCount, paidAmount));
        }
        return map;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return 0L;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private static class OrderSummary {
        private final Long orderCount;
        private final BigDecimal paidAmount;

        private OrderSummary(Long orderCount, BigDecimal paidAmount) {
            this.orderCount = orderCount;
            this.paidAmount = paidAmount;
        }

        private static OrderSummary empty() {
            return new OrderSummary(0L, BigDecimal.ZERO);
        }
    }
}
