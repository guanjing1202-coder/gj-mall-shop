package com.gj.mall.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.admin.service.AdminDashboardService;
import com.gj.mall.admin.vo.DashboardBusinessVO;
import com.gj.mall.admin.vo.DashboardOverviewVO;
import com.gj.mall.marketing.entity.SmsCoupon;
import com.gj.mall.marketing.entity.SmsSeckill;
import com.gj.mall.marketing.mapper.SmsCouponMapper;
import com.gj.mall.marketing.mapper.SmsSeckillMapper;
import com.gj.mall.order.entity.OmsAfterSale;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsAfterSaleMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.entity.PmsBrand;
import com.gj.mall.product.entity.PmsCategory;
import com.gj.mall.product.entity.PmsProductComment;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsBrandMapper;
import com.gj.mall.product.mapper.PmsCategoryMapper;
import com.gj.mall.product.mapper.PmsProductCommentMapper;
import com.gj.mall.product.mapper.PmsSkuMapper;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.product.vo.AdminInventoryVO;
import com.gj.mall.user.entity.UmsUser;
import com.gj.mall.user.mapper.UmsUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final PmsSpuMapper spuMapper;
    private final PmsBrandMapper brandMapper;
    private final PmsCategoryMapper categoryMapper;
    private final PmsSkuMapper skuMapper;
    private final PmsProductCommentMapper commentMapper;
    private final UmsUserMapper userMapper;
    private final OmsOrderMapper orderMapper;
    private final OmsAfterSaleMapper afterSaleMapper;
    private final SmsCouponMapper couponMapper;
    private final SmsSeckillMapper seckillMapper;

    @Override
    public DashboardOverviewVO overview() {
        LocalDateTime todayStart = LocalDate.now().atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);

        DashboardOverviewVO vo = new DashboardOverviewVO();
        fillProductStats(vo);
        fillUserStats(vo, todayStart, tomorrowStart);
        fillOrderStats(vo, todayStart, tomorrowStart);
        return vo;
    }

    @Override
    public DashboardBusinessVO business() {
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime tomorrowStart = todayStart.plusDays(1);
        LocalDateTime now = LocalDateTime.now();

        DashboardBusinessVO vo = new DashboardBusinessVO();
        fillBusinessProductStats(vo);
        fillBusinessUserStats(vo, todayStart, tomorrowStart);
        fillBusinessOrderStats(vo, todayStart, tomorrowStart);
        fillBusinessPendingStats(vo);
        fillBusinessMarketingStats(vo, now);
        vo.setOrderTrend(buildOrderTrend(today));
        vo.setHotProducts(buildHotProducts());
        vo.setLowStockSkus(buildLowStockSkus());
        vo.setLatestOrders(buildLatestOrders());
        return vo;
    }

    private void fillProductStats(DashboardOverviewVO vo) {
        vo.setProductTotal(spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()));
        vo.setProductOnSale(spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()
                .eq(PmsSpu::getPublishStatus, 1)));
        vo.setBrandTotal(brandMapper.selectCount(Wrappers.<PmsBrand>lambdaQuery()));
        vo.setCategoryTotal(categoryMapper.selectCount(Wrappers.<PmsCategory>lambdaQuery()));
    }

    private void fillUserStats(DashboardOverviewVO vo, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        vo.setUserTotal(userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()));
        vo.setTodayNewUsers(userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()
                .ge(UmsUser::getCreateTime, todayStart)
                .lt(UmsUser::getCreateTime, tomorrowStart)));
    }

    private void fillOrderStats(DashboardOverviewVO vo, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        vo.setOrderTotal(orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()));
        vo.setTodayOrderCount(orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()
                .ge(OmsOrder::getCreateTime, todayStart)
                .lt(OmsOrder::getCreateTime, tomorrowStart)));
        vo.setPendingPayCount(countOrderByStatus(OrderStatus.PENDING_PAY));
        vo.setPendingDeliveryCount(countOrderByStatus(OrderStatus.PENDING_DELIVERY));
        vo.setPendingReceiveCount(countOrderByStatus(OrderStatus.PENDING_RECEIVE));
        vo.setCompletedCount(countOrderByStatus(OrderStatus.COMPLETED));
        vo.setCanceledCount(countOrderByStatus(OrderStatus.CANCELED));
        vo.setPaidAmountTotal(sumPayAmount(null, null));
        vo.setTodayPaidAmount(sumPayAmount(todayStart, tomorrowStart));
    }

    private void fillBusinessProductStats(DashboardBusinessVO vo) {
        vo.setProductTotal(spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()));
        vo.setProductOnSale(spuMapper.selectCount(Wrappers.<PmsSpu>lambdaQuery()
                .eq(PmsSpu::getPublishStatus, 1)));
        vo.setBrandTotal(brandMapper.selectCount(Wrappers.<PmsBrand>lambdaQuery()));
        vo.setCategoryTotal(categoryMapper.selectCount(Wrappers.<PmsCategory>lambdaQuery()));

        AdminInventorySummaryVO inventorySummary = skuMapper.selectInventorySummary(new AdminInventoryQueryDTO());
        vo.setLowStockSkuCount(inventorySummary == null ? 0L : safeLong(inventorySummary.getLowSkuCount()));
        vo.setEmptyStockSkuCount(inventorySummary == null ? 0L : safeLong(inventorySummary.getEmptySkuCount()));
    }

    private void fillBusinessUserStats(DashboardBusinessVO vo, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        vo.setUserTotal(userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()));
        vo.setTodayNewUsers(userMapper.selectCount(Wrappers.<UmsUser>lambdaQuery()
                .ge(UmsUser::getCreateTime, todayStart)
                .lt(UmsUser::getCreateTime, tomorrowStart)));
    }

    private void fillBusinessOrderStats(DashboardBusinessVO vo, LocalDateTime todayStart, LocalDateTime tomorrowStart) {
        vo.setOrderTotal(orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()));
        vo.setTodayOrderCount(orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()
                .ge(OmsOrder::getCreateTime, todayStart)
                .lt(OmsOrder::getCreateTime, tomorrowStart)));
        vo.setPendingPayCount(countOrderByStatus(OrderStatus.PENDING_PAY));
        vo.setPendingDeliveryCount(countOrderByStatus(OrderStatus.PENDING_DELIVERY));
        vo.setPendingReceiveCount(countOrderByStatus(OrderStatus.PENDING_RECEIVE));
        vo.setCompletedCount(countOrderByStatus(OrderStatus.COMPLETED));
        vo.setCanceledCount(countOrderByStatus(OrderStatus.CANCELED));
        vo.setRefundingCount(countOrderByStatus(OrderStatus.REFUNDING));
        vo.setRefundedCount(countOrderByStatus(OrderStatus.REFUNDED));
        vo.setPaidAmountTotal(sumPayAmount(null, null));
        vo.setTodayPaidAmount(sumPayAmount(todayStart, tomorrowStart));
        vo.setPendingDeliveryAmount(sumPayAmountByStatuses(Arrays.asList(OrderStatus.PENDING_DELIVERY.getCode()), null, null));
    }

    private void fillBusinessPendingStats(DashboardBusinessVO vo) {
        vo.setPendingAfterSaleCount(afterSaleMapper.selectCount(Wrappers.<OmsAfterSale>lambdaQuery()
                .in(OmsAfterSale::getStatus, Arrays.asList(0, 1, 2))));
        vo.setPendingCommentCount(commentMapper.selectCount(Wrappers.<PmsProductComment>lambdaQuery()
                .eq(PmsProductComment::getStatus, 0)));
    }

    private void fillBusinessMarketingStats(DashboardBusinessVO vo, LocalDateTime now) {
        vo.setActiveCouponCount(couponMapper.selectCount(Wrappers.<SmsCoupon>lambdaQuery()
                .eq(SmsCoupon::getStatus, 1)
                .le(SmsCoupon::getStartTime, now)
                .ge(SmsCoupon::getEndTime, now)));
        vo.setActiveSeckillCount(seckillMapper.selectCount(Wrappers.<SmsSeckill>lambdaQuery()
                .eq(SmsSeckill::getStatus, 1)
                .le(SmsSeckill::getStartTime, now)
                .ge(SmsSeckill::getEndTime, now)));
    }

    private List<DashboardBusinessVO.TrendItem> buildOrderTrend(LocalDate today) {
        List<DashboardBusinessVO.TrendItem> result = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime start = date.atStartOfDay();
            LocalDateTime end = start.plusDays(1);
            DashboardBusinessVO.TrendItem item = new DashboardBusinessVO.TrendItem();
            item.setDate(date);
            item.setOrderCount(orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()
                    .ge(OmsOrder::getCreateTime, start)
                    .lt(OmsOrder::getCreateTime, end)));
            item.setPaidAmount(sumPayAmount(start, end));
            result.add(item);
        }
        return result;
    }

    private List<DashboardBusinessVO.HotProductItem> buildHotProducts() {
        Page<PmsSpu> page = spuMapper.selectPage(new Page<>(1, 5),
                Wrappers.<PmsSpu>lambdaQuery()
                        .eq(PmsSpu::getPublishStatus, 1)
                        .orderByDesc(PmsSpu::getSaleCount)
                        .orderByDesc(PmsSpu::getId));
        return page.getRecords().stream().map(spu -> {
            DashboardBusinessVO.HotProductItem item = new DashboardBusinessVO.HotProductItem();
            item.setId(spu.getId());
            item.setName(spu.getName());
            item.setMainImage(spu.getMainImage());
            item.setPrice(spu.getPrice());
            item.setSaleCount(spu.getSaleCount());
            return item;
        }).collect(Collectors.toList());
    }

    private List<DashboardBusinessVO.LowStockItem> buildLowStockSkus() {
        AdminInventoryQueryDTO query = new AdminInventoryQueryDTO();
        query.setStockStatus("low");
        IPage<AdminInventoryVO> page = skuMapper.selectInventoryPage(new Page<>(1, 5), query);
        return page.getRecords().stream().map(sku -> {
            DashboardBusinessVO.LowStockItem item = new DashboardBusinessVO.LowStockItem();
            item.setSkuId(sku.getSkuId());
            item.setSpuId(sku.getSpuId());
            item.setSpuName(sku.getSpuName());
            item.setSkuName(sku.getSkuName());
            item.setSkuCode(sku.getSkuCode());
            item.setStock(sku.getStock());
            item.setWarnStock(sku.getWarnStock());
            item.setAlertGap(Math.max(safeInt(sku.getWarnStock()) - safeInt(sku.getStock()), 0));
            return item;
        }).collect(Collectors.toList());
    }

    private List<DashboardBusinessVO.LatestOrderItem> buildLatestOrders() {
        Page<OmsOrder> page = orderMapper.selectPage(new Page<>(1, 6),
                Wrappers.<OmsOrder>lambdaQuery().orderByDesc(OmsOrder::getCreateTime));
        return page.getRecords().stream().map(order -> {
            DashboardBusinessVO.LatestOrderItem item = new DashboardBusinessVO.LatestOrderItem();
            item.setId(order.getId());
            item.setOrderNo(order.getOrderNo());
            item.setUserId(order.getUserId());
            item.setPayAmount(order.getPayAmount());
            item.setStatus(order.getStatus());
            OrderStatus status = OrderStatus.of(order.getStatus());
            item.setStatusDesc(status == null ? "未知" : status.getDesc());
            item.setCreateTime(order.getCreateTime());
            return item;
        }).collect(Collectors.toList());
    }

    private Long countOrderByStatus(OrderStatus status) {
        return orderMapper.selectCount(Wrappers.<OmsOrder>lambdaQuery()
                .eq(OmsOrder::getStatus, status.getCode()));
    }

    private BigDecimal sumPayAmount(LocalDateTime start, LocalDateTime end) {
        List<Integer> paidStatuses = Arrays.asList(
                OrderStatus.PENDING_DELIVERY.getCode(),
                OrderStatus.PENDING_RECEIVE.getCode(),
                OrderStatus.COMPLETED.getCode()
        );
        return sumPayAmountByStatuses(paidStatuses, start, end);
    }

    private BigDecimal sumPayAmountByStatuses(List<Integer> statuses, LocalDateTime start, LocalDateTime end) {
        QueryWrapper<OmsOrder> wrapper = Wrappers.query();
        wrapper.select("COALESCE(SUM(pay_amount), 0)");
        wrapper.in("status", statuses);
        if (start != null) {
            wrapper.ge("pay_time", start);
        }
        if (end != null) {
            wrapper.lt("pay_time", end);
        }
        List<Object> rows = orderMapper.selectObjs(wrapper);
        if (rows == null || rows.isEmpty() || rows.get(0) == null) {
            return BigDecimal.ZERO;
        }
        Object value = rows.get(0);
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

    private long safeLong(Long value) {
        return value == null ? 0L : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }
}
