package com.gj.mall.order.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.AdminDeliveryCompanyQueryDTO;
import com.gj.mall.order.dto.AdminDeliveryCompanySaveDTO;
import com.gj.mall.order.dto.AdminLogisticsOrderQueryDTO;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.entity.OmsDeliveryCompany;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsDeliveryCompanyMapper;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.service.LogisticsService;
import com.gj.mall.order.service.OrderService;
import com.gj.mall.order.vo.AdminDeliveryCompanyVO;
import com.gj.mall.order.vo.AdminLogisticsSummaryVO;
import com.gj.mall.order.vo.OrderItemVO;
import com.gj.mall.order.vo.OrderVO;
import com.gj.mall.order.vo.ReceiverVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LogisticsServiceImpl implements LogisticsService {

    private final OmsOrderMapper orderMapper;
    private final OmsOrderItemMapper itemMapper;
    private final OmsDeliveryCompanyMapper companyMapper;
    private final OrderService orderService;

    @Override
    public AdminLogisticsSummaryVO summary() {
        AdminLogisticsSummaryVO summary = orderMapper.selectLogisticsSummary();
        if (summary == null) {
            summary = new AdminLogisticsSummaryVO();
        }
        summary.setActiveCompanyCount(companyMapper.selectCount(Wrappers.<OmsDeliveryCompany>lambdaQuery()
                .eq(OmsDeliveryCompany::getStatus, 1)));
        return summary;
    }

    @Override
    public PageResult<OrderVO> orderPage(AdminLogisticsOrderQueryDTO query) {
        Page<OmsOrder> page = new Page<>(safe(query.getPageNum(), 1L), safe(query.getPageSize(), 10L));
        IPage<OmsOrder> result = orderMapper.selectPage(page,
                Wrappers.<OmsOrder>lambdaQuery()
                        .eq(query.getUserId() != null, OmsOrder::getUserId, query.getUserId())
                        .like(StrUtil.isNotBlank(query.getOrderNo()), OmsOrder::getOrderNo, StrUtil.trim(query.getOrderNo()))
                        .like(StrUtil.isNotBlank(query.getDeliveryNo()), OmsOrder::getDeliveryNo, StrUtil.trim(query.getDeliveryNo()))
                        .like(StrUtil.isNotBlank(query.getDeliveryCompany()), OmsOrder::getDeliveryCompany, StrUtil.trim(query.getDeliveryCompany()))
                        .eq(query.getStatus() != null, OmsOrder::getStatus, query.getStatus())
                        .in(query.getStatus() == null, OmsOrder::getStatus, Arrays.asList(
                                OrderStatus.PENDING_DELIVERY.getCode(),
                                OrderStatus.PENDING_RECEIVE.getCode()))
                        .orderByAsc(OmsOrder::getStatus)
                        .orderByDesc(OmsOrder::getId));
        return toPageVO(result);
    }

    @Override
    public OrderVO deliver(Long orderId, AdminOrderDeliverDTO dto) {
        return orderService.deliver(orderId, dto);
    }

    @Override
    public PageResult<AdminDeliveryCompanyVO> companyPage(AdminDeliveryCompanyQueryDTO query) {
        Page<OmsDeliveryCompany> page = new Page<>(safe(query.getPageNum(), 1L), safe(query.getPageSize(), 10L));
        IPage<OmsDeliveryCompany> result = companyMapper.selectPage(page,
                Wrappers.<OmsDeliveryCompany>lambdaQuery()
                        .and(StrUtil.isNotBlank(query.getKeyword()), w -> w
                                .like(OmsDeliveryCompany::getCode, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(OmsDeliveryCompany::getName, StrUtil.trim(query.getKeyword()))
                                .or()
                                .like(OmsDeliveryCompany::getContactPhone, StrUtil.trim(query.getKeyword())))
                        .eq(query.getStatus() != null, OmsDeliveryCompany::getStatus, query.getStatus())
                        .orderByAsc(OmsDeliveryCompany::getSort)
                        .orderByDesc(OmsDeliveryCompany::getUpdateTime)
                        .orderByDesc(OmsDeliveryCompany::getId));
        if (CollUtil.isEmpty(result.getRecords())) {
            return PageResult.empty(result.getCurrent(), result.getSize());
        }
        return new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords().stream().map(AdminDeliveryCompanyVO::from).collect(Collectors.toList()));
    }

    @Override
    public List<AdminDeliveryCompanyVO> enabledCompanies() {
        return companyMapper.selectList(Wrappers.<OmsDeliveryCompany>lambdaQuery()
                        .eq(OmsDeliveryCompany::getStatus, 1)
                        .orderByAsc(OmsDeliveryCompany::getSort)
                        .orderByAsc(OmsDeliveryCompany::getId))
                .stream()
                .map(AdminDeliveryCompanyVO::from)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createCompany(AdminDeliveryCompanySaveDTO dto) {
        validateCompany(dto);
        ensureCompanyCodeUnique(dto.getCode(), null);
        OmsDeliveryCompany company = new OmsDeliveryCompany();
        fillCompany(company, dto);
        company.setId(null);
        companyMapper.insert(company);
        return company.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateCompany(AdminDeliveryCompanySaveDTO dto) {
        if (dto.getId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "缺少物流公司ID");
        }
        OmsDeliveryCompany exists = companyMapper.selectById(dto.getId());
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "物流公司不存在");
        }
        validateCompany(dto);
        ensureCompanyCodeUnique(dto.getCode(), dto.getId());
        OmsDeliveryCompany update = new OmsDeliveryCompany();
        fillCompany(update, dto);
        update.setId(dto.getId());
        companyMapper.updateById(update);
    }

    @Override
    public void updateCompanyStatus(Long id, Integer status) {
        if (status == null || (status != 0 && status != 1)) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
        OmsDeliveryCompany exists = companyMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "物流公司不存在");
        }
        OmsDeliveryCompany update = new OmsDeliveryCompany();
        update.setId(id);
        update.setStatus(status);
        companyMapper.updateById(update);
    }

    @Override
    public void deleteCompany(Long id) {
        OmsDeliveryCompany exists = companyMapper.selectById(id);
        if (exists == null) {
            throw new BizException(ResultCode.DATA_NOT_FOUND, "物流公司不存在");
        }
        companyMapper.deleteById(id);
    }

    private PageResult<OrderVO> toPageVO(IPage<OmsOrder> page) {
        if (CollUtil.isEmpty(page.getRecords())) {
            return PageResult.empty(page.getCurrent(), page.getSize());
        }
        List<Long> orderIds = page.getRecords().stream().map(OmsOrder::getId).collect(Collectors.toList());
        Map<Long, List<OrderItemVO>> grouped = new LinkedHashMap<>();
        List<OmsOrderItem> items = itemMapper.selectList(Wrappers.<OmsOrderItem>lambdaQuery()
                .in(OmsOrderItem::getOrderId, orderIds));
        for (OmsOrderItem item : items) {
            grouped.computeIfAbsent(item.getOrderId(), key -> new ArrayList<>()).add(toItemVO(item));
        }
        List<OrderVO> records = page.getRecords().stream()
                .map(order -> toOrderVO(order, grouped.getOrDefault(order.getId(), Collections.emptyList())))
                .collect(Collectors.toList());
        return new PageResult<>(page.getTotal(), page.getCurrent(), page.getSize(), records);
    }

    private OrderItemVO toItemVO(OmsOrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        BeanUtil.copyProperties(item, vo, "specData");
        if (StrUtil.isNotBlank(item.getSpecData())) {
            try {
                vo.setSpecData(JSON.parseObject(item.getSpecData(), new TypeReference<Map<String, String>>() {}));
            } catch (Exception ignored) {
            }
        }
        return vo;
    }

    private OrderVO toOrderVO(OmsOrder order, List<OrderItemVO> items) {
        OrderVO vo = new OrderVO();
        BeanUtil.copyProperties(order, vo, "receiverInfo");
        OrderStatus status = OrderStatus.of(order.getStatus());
        vo.setStatusDesc(status == null ? "未知" : status.getDesc());
        if (StrUtil.isNotBlank(order.getReceiverInfo())) {
            try {
                vo.setReceiver(JSON.parseObject(order.getReceiverInfo(), ReceiverVO.class));
            } catch (Exception ignored) {
            }
        }
        vo.setItems(items);
        return vo;
    }

    private void fillCompany(OmsDeliveryCompany company, AdminDeliveryCompanySaveDTO dto) {
        company.setCode(StrUtil.trim(dto.getCode()).toUpperCase());
        company.setName(StrUtil.trim(dto.getName()));
        company.setContactPhone(StrUtil.trim(dto.getContactPhone()));
        company.setSort(dto.getSort() == null ? 100 : dto.getSort());
        company.setStatus(dto.getStatus() == null ? 1 : dto.getStatus());
    }

    private void validateCompany(AdminDeliveryCompanySaveDTO dto) {
        String code = StrUtil.trim(dto.getCode());
        if (StrUtil.isBlank(code) || !code.matches("^[A-Za-z][A-Za-z0-9_-]{1,31}$")) {
            throw new BizException(ResultCode.PARAM_ERROR, "物流公司编码格式不正确");
        }
        if (dto.getStatus() != null && dto.getStatus() != 0 && dto.getStatus() != 1) {
            throw new BizException(ResultCode.PARAM_ERROR, "状态值非法");
        }
    }

    private void ensureCompanyCodeUnique(String code, Long selfId) {
        Long count = companyMapper.selectCount(Wrappers.<OmsDeliveryCompany>lambdaQuery()
                .eq(OmsDeliveryCompany::getCode, StrUtil.trim(code).toUpperCase())
                .ne(selfId != null, OmsDeliveryCompany::getId, selfId));
        if (count != null && count > 0) {
            throw new BizException(ResultCode.DATA_EXISTS, "物流公司编码已存在");
        }
    }

    private long safe(Long value, long fallback) {
        return value == null || value <= 0 ? fallback : value;
    }
}
