package com.gj.mall.product.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.product.dto.AdminInventoryQueryDTO;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.vo.AdminInventorySummaryVO;
import com.gj.mall.product.vo.AdminInventoryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface PmsSkuMapper extends BaseMapper<PmsSku> {

    @Select({
            "<script>",
            "SELECT",
            "  sku.id AS sku_id,",
            "  sku.spu_id AS spu_id,",
            "  spu.name AS spu_name,",
            "  spu.sub_title AS sub_title,",
            "  sku.sku_code AS sku_code,",
            "  sku.name AS sku_name,",
            "  spu.main_image AS main_image,",
            "  sku.image AS sku_image,",
            "  brand.id AS brand_id,",
            "  brand.name AS brand_name,",
            "  category.id AS category_id,",
            "  category.name AS category_name,",
            "  sku.price AS price,",
            "  sku.stock AS stock,",
            "  sku.locked_stock AS locked_stock,",
            "  sku.warn_stock AS warn_stock,",
            "  (IFNULL(sku.stock, 0) + IFNULL(sku.locked_stock, 0)) AS total_stock,",
            "  sku.sale_count AS sale_count,",
            "  spu.publish_status AS publish_status,",
            "  sku.spec_data AS spec_data_json,",
            "  sku.update_time AS update_time",
            "FROM pms_sku sku",
            "JOIN pms_spu spu ON sku.spu_id = spu.id AND spu.deleted = 0",
            "LEFT JOIN pms_brand brand ON spu.brand_id = brand.id AND brand.deleted = 0",
            "LEFT JOIN pms_category category ON spu.category_id = category.id AND category.deleted = 0",
            "WHERE sku.deleted = 0",
            "<if test='query.spuId != null'>AND sku.spu_id = #{query.spuId}</if>",
            "<if test='query.brandId != null'>AND spu.brand_id = #{query.brandId}</if>",
            "<if test='query.categoryId != null'>AND spu.category_id = #{query.categoryId}</if>",
            "<if test='query.publishStatus != null'>AND spu.publish_status = #{query.publishStatus}</if>",
            "<if test='query.keyword != null and query.keyword != \"\"'>",
            "  AND (sku.name LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR sku.sku_code LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR spu.name LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR spu.sub_title LIKE CONCAT('%', #{query.keyword}, '%'))",
            "</if>",
            "<choose>",
            "  <when test='query.stockStatus == \"empty\"'>AND IFNULL(sku.stock, 0) = 0</when>",
            "  <when test='query.stockStatus == \"low\"'>AND IFNULL(sku.stock, 0) &gt; 0 AND IFNULL(sku.stock, 0) &lt;= IFNULL(sku.warn_stock, 10)</when>",
            "  <when test='query.stockStatus == \"normal\"'>AND IFNULL(sku.stock, 0) &gt; IFNULL(sku.warn_stock, 10)</when>",
            "  <when test='query.stockStatus == \"locked\"'>AND IFNULL(sku.locked_stock, 0) &gt; 0</when>",
            "</choose>",
            "ORDER BY CASE WHEN IFNULL(sku.stock, 0) = 0 THEN 0 WHEN IFNULL(sku.stock, 0) &lt;= IFNULL(sku.warn_stock, 10) THEN 1 ELSE 2 END, IFNULL(sku.stock, 0) ASC, sku.update_time DESC, sku.id DESC",
            "</script>"
    })
    IPage<AdminInventoryVO> selectInventoryPage(Page<AdminInventoryVO> page,
                                                @Param("query") AdminInventoryQueryDTO query);

    @Select({
            "SELECT",
            "  sku.id AS sku_id,",
            "  sku.spu_id AS spu_id,",
            "  spu.name AS spu_name,",
            "  spu.sub_title AS sub_title,",
            "  sku.sku_code AS sku_code,",
            "  sku.name AS sku_name,",
            "  spu.main_image AS main_image,",
            "  sku.image AS sku_image,",
            "  brand.id AS brand_id,",
            "  brand.name AS brand_name,",
            "  category.id AS category_id,",
            "  category.name AS category_name,",
            "  sku.price AS price,",
            "  sku.stock AS stock,",
            "  sku.locked_stock AS locked_stock,",
            "  sku.warn_stock AS warn_stock,",
            "  (IFNULL(sku.stock, 0) + IFNULL(sku.locked_stock, 0)) AS total_stock,",
            "  sku.sale_count AS sale_count,",
            "  spu.publish_status AS publish_status,",
            "  sku.spec_data AS spec_data_json,",
            "  sku.update_time AS update_time",
            "FROM pms_sku sku",
            "JOIN pms_spu spu ON sku.spu_id = spu.id AND spu.deleted = 0",
            "LEFT JOIN pms_brand brand ON spu.brand_id = brand.id AND brand.deleted = 0",
            "LEFT JOIN pms_category category ON spu.category_id = category.id AND category.deleted = 0",
            "WHERE sku.deleted = 0 AND sku.id = #{skuId}"
    })
    AdminInventoryVO selectInventoryBySkuId(@Param("skuId") Long skuId);

    @Select({
            "<script>",
            "SELECT",
            "  COUNT(1) AS total_sku_count,",
            "  COALESCE(SUM(CASE WHEN IFNULL(sku.stock, 0) = 0 THEN 1 ELSE 0 END), 0) AS empty_sku_count,",
            "  COALESCE(SUM(CASE WHEN IFNULL(sku.stock, 0) &gt; 0 AND IFNULL(sku.stock, 0) &lt;= IFNULL(sku.warn_stock, 10) THEN 1 ELSE 0 END), 0) AS low_sku_count,",
            "  COALESCE(SUM(CASE WHEN IFNULL(sku.locked_stock, 0) &gt; 0 THEN 1 ELSE 0 END), 0) AS locked_sku_count,",
            "  COALESCE(SUM(IFNULL(sku.stock, 0)), 0) AS total_available_stock,",
            "  COALESCE(SUM(IFNULL(sku.locked_stock, 0)), 0) AS total_locked_stock,",
            "  COALESCE(SUM(IFNULL(sku.stock, 0) + IFNULL(sku.locked_stock, 0)), 0) AS total_stock,",
            "  COALESCE(SUM(IFNULL(sku.stock, 0) * IFNULL(sku.price, 0)), 0) AS stock_amount",
            "FROM pms_sku sku",
            "JOIN pms_spu spu ON sku.spu_id = spu.id AND spu.deleted = 0",
            "LEFT JOIN pms_brand brand ON spu.brand_id = brand.id AND brand.deleted = 0",
            "LEFT JOIN pms_category category ON spu.category_id = category.id AND category.deleted = 0",
            "WHERE sku.deleted = 0",
            "<if test='query.spuId != null'>AND sku.spu_id = #{query.spuId}</if>",
            "<if test='query.brandId != null'>AND spu.brand_id = #{query.brandId}</if>",
            "<if test='query.categoryId != null'>AND spu.category_id = #{query.categoryId}</if>",
            "<if test='query.publishStatus != null'>AND spu.publish_status = #{query.publishStatus}</if>",
            "<if test='query.keyword != null and query.keyword != \"\"'>",
            "  AND (sku.name LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR sku.sku_code LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR spu.name LIKE CONCAT('%', #{query.keyword}, '%')",
            "    OR spu.sub_title LIKE CONCAT('%', #{query.keyword}, '%'))",
            "</if>",
            "<choose>",
            "  <when test='query.stockStatus == \"empty\"'>AND IFNULL(sku.stock, 0) = 0</when>",
            "  <when test='query.stockStatus == \"low\"'>AND IFNULL(sku.stock, 0) &gt; 0 AND IFNULL(sku.stock, 0) &lt;= IFNULL(sku.warn_stock, 10)</when>",
            "  <when test='query.stockStatus == \"normal\"'>AND IFNULL(sku.stock, 0) &gt; IFNULL(sku.warn_stock, 10)</when>",
            "  <when test='query.stockStatus == \"locked\"'>AND IFNULL(sku.locked_stock, 0) &gt; 0</when>",
            "</choose>",
            "</script>"
    })
    AdminInventorySummaryVO selectInventorySummary(@Param("query") AdminInventoryQueryDTO query);

    @Update("UPDATE pms_sku SET stock = stock + #{delta}, update_time = NOW() WHERE id = #{skuId} AND deleted = 0")
    int increaseStock(@Param("skuId") Long skuId, @Param("delta") Integer delta);

    @Update("UPDATE pms_sku SET stock = stock - #{delta}, update_time = NOW() WHERE id = #{skuId} AND deleted = 0 AND stock >= #{delta}")
    int decreaseStock(@Param("skuId") Long skuId, @Param("delta") Integer delta);

    @Update("UPDATE pms_sku SET warn_stock = #{warnStock}, update_time = NOW() WHERE id = #{skuId} AND deleted = 0")
    int updateWarnStock(@Param("skuId") Long skuId, @Param("warnStock") Integer warnStock);

    @Select("SELECT * FROM pms_sku WHERE id = #{id} AND deleted = 0 FOR UPDATE")
    PmsSku selectByIdForUpdate(@Param("id") Long id);
}
