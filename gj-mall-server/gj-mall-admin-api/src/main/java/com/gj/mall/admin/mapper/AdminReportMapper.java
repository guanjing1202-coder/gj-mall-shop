package com.gj.mall.admin.mapper;

import com.gj.mall.admin.vo.AdminSalesReportVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AdminReportMapper {

    @Select({
            "SELECT",
            "  COUNT(1) AS order_count,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3, 5, 6) THEN 1 ELSE 0 END), 0) AS paid_order_count,",
            "  COALESCE(SUM(CASE WHEN status = 6 THEN 1 ELSE 0 END), 0) AS refund_order_count,",
            "  COALESCE(SUM(total_amount), 0) AS gross_amount,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3, 5, 6) THEN pay_amount ELSE 0 END), 0) AS paid_amount",
            "FROM oms_order",
            "WHERE deleted = 0",
            "  AND create_time >= #{start}",
            "  AND create_time < #{end}"
    })
    AdminSalesReportVO.Overview selectOverview(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT",
            "  COUNT(1)",
            "FROM ums_user",
            "WHERE deleted = 0",
            "  AND create_time >= #{start}",
            "  AND create_time < #{end}"
    })
    Long selectNewMemberCount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT",
            "  DATE(create_time) AS date,",
            "  COUNT(1) AS order_count,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3, 5, 6) THEN 1 ELSE 0 END), 0) AS paid_order_count,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3, 5, 6) THEN pay_amount ELSE 0 END), 0) AS paid_amount,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3, 5) THEN pay_amount ELSE 0 END), 0) AS net_amount",
            "FROM oms_order",
            "WHERE deleted = 0",
            "  AND create_time >= #{start}",
            "  AND create_time < #{end}",
            "GROUP BY DATE(create_time)",
            "ORDER BY DATE(create_time)"
    })
    List<AdminSalesReportVO.TrendItem> selectSalesTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT",
            "  oi.spu_id AS spu_id,",
            "  COALESCE(spu.name, oi.sku_name) AS product_name,",
            "  COALESCE(SUM(oi.quantity), 0) AS sale_quantity,",
            "  COUNT(DISTINCT oi.order_id) AS order_count,",
            "  COALESCE(SUM(oi.total_amount), 0) AS sales_amount,",
            "  COALESCE(SUM(oi.total_amount) / NULLIF(SUM(oi.quantity), 0), 0) AS average_price",
            "FROM oms_order_item oi",
            "JOIN oms_order o ON o.id = oi.order_id AND o.deleted = 0",
            "LEFT JOIN pms_spu spu ON spu.id = oi.spu_id AND spu.deleted = 0",
            "WHERE o.status IN (1, 2, 3, 5, 6)",
            "  AND o.create_time >= #{start}",
            "  AND o.create_time < #{end}",
            "GROUP BY oi.spu_id, COALESCE(spu.name, oi.sku_name)",
            "ORDER BY sale_quantity DESC, sales_amount DESC",
            "LIMIT #{limit}"
    })
    List<AdminSalesReportVO.ProductRankItem> selectProductRanks(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    @Select({
            "SELECT",
            "  spu.category_id AS category_id,",
            "  COALESCE(c.name, '未分类') AS category_name,",
            "  COALESCE(SUM(oi.quantity), 0) AS sale_quantity,",
            "  COUNT(DISTINCT oi.order_id) AS order_count,",
            "  COALESCE(SUM(oi.total_amount), 0) AS sales_amount",
            "FROM oms_order_item oi",
            "JOIN oms_order o ON o.id = oi.order_id AND o.deleted = 0",
            "LEFT JOIN pms_spu spu ON spu.id = oi.spu_id AND spu.deleted = 0",
            "LEFT JOIN pms_category c ON c.id = spu.category_id AND c.deleted = 0",
            "WHERE o.status IN (1, 2, 3, 5, 6)",
            "  AND o.create_time >= #{start}",
            "  AND o.create_time < #{end}",
            "GROUP BY spu.category_id, COALESCE(c.name, '未分类')",
            "ORDER BY sales_amount DESC, sale_quantity DESC",
            "LIMIT #{limit}"
    })
    List<AdminSalesReportVO.CategoryRankItem> selectCategoryRanks(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("limit") int limit
    );

    @Select({
            "SELECT",
            "  DATE(update_time) AS date,",
            "  COUNT(1) AS refund_count,",
            "  COALESCE(SUM(amount), 0) AS refund_amount",
            "FROM pay_payment_record",
            "WHERE status = 3",
            "  AND update_time >= #{start}",
            "  AND update_time < #{end}",
            "GROUP BY DATE(update_time)",
            "ORDER BY DATE(update_time)"
    })
    List<AdminSalesReportVO.RefundTrendItem> selectRefundTrend(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT",
            "  DATE(create_time) AS date,",
            "  COUNT(1) AS new_member_count",
            "FROM ums_user",
            "WHERE deleted = 0",
            "  AND create_time >= #{start}",
            "  AND create_time < #{end}",
            "GROUP BY DATE(create_time)",
            "ORDER BY DATE(create_time)"
    })
    List<AdminSalesReportVO.MemberGrowthItem> selectMemberGrowth(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Select({
            "SELECT COALESCE(SUM(amount), 0)",
            "FROM pay_payment_record",
            "WHERE status = 3",
            "  AND update_time >= #{start}",
            "  AND update_time < #{end}"
    })
    java.math.BigDecimal selectRefundAmount(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);
}
