package com.gj.mall.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.vo.AdminOrderFulfillmentSummaryVO;
import com.gj.mall.order.vo.AdminLogisticsSummaryVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface OmsOrderMapper extends BaseMapper<OmsOrder> {

    @Select({
            "SELECT",
            "  COUNT(1) AS total_order_count,",
            "  COALESCE(SUM(CASE WHEN status = 0 THEN 1 ELSE 0 END), 0) AS pending_pay_count,",
            "  COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS pending_delivery_count,",
            "  COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS pending_receive_count,",
            "  COALESCE(SUM(CASE WHEN status = 3 THEN 1 ELSE 0 END), 0) AS completed_count,",
            "  COALESCE(SUM(CASE WHEN status = 4 THEN 1 ELSE 0 END), 0) AS canceled_count,",
            "  COALESCE(SUM(CASE WHEN status = 5 THEN 1 ELSE 0 END), 0) AS refunding_count,",
            "  COALESCE(SUM(CASE WHEN status = 6 THEN 1 ELSE 0 END), 0) AS refunded_count,",
            "  COALESCE(SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END), 0) AS today_order_count,",
            "  COALESCE(SUM(CASE WHEN status = 1 THEN pay_amount ELSE 0 END), 0) AS pending_delivery_amount,",
            "  COALESCE(SUM(CASE WHEN status IN (1, 2, 3) THEN pay_amount ELSE 0 END), 0) AS total_pay_amount",
            "FROM oms_order",
            "WHERE deleted = 0"
    })
    AdminOrderFulfillmentSummaryVO selectFulfillmentSummary();

    @Select({
            "SELECT",
            "  COALESCE(SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END), 0) AS pending_delivery_count,",
            "  COALESCE(SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END), 0) AS pending_receive_count,",
            "  COALESCE(SUM(CASE WHEN status = 2 AND DATE(delivery_time) = CURDATE() THEN 1 ELSE 0 END), 0) AS shipped_today_count,",
            "  COALESCE(SUM(CASE WHEN status = 3 AND DATE(receive_time) = CURDATE() THEN 1 ELSE 0 END), 0) AS received_today_count,",
            "  COALESCE(SUM(CASE WHEN status = 1 THEN pay_amount ELSE 0 END), 0) AS pending_delivery_amount,",
            "  COALESCE(SUM(CASE WHEN status = 2 THEN pay_amount ELSE 0 END), 0) AS pending_receive_amount,",
            "  COALESCE(SUM(CASE WHEN status = 2 AND DATE(delivery_time) = CURDATE() THEN pay_amount ELSE 0 END), 0) AS shipped_today_amount,",
            "  COALESCE(SUM(CASE WHEN status = 3 AND DATE(receive_time) = CURDATE() THEN pay_amount ELSE 0 END), 0) AS received_today_amount,",
            "  COALESCE(SUM(CASE WHEN status = 1 AND pay_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN 1 ELSE 0 END), 0) AS overdue_delivery_count,",
            "  COALESCE(SUM(CASE WHEN status = 2 AND delivery_time < DATE_SUB(NOW(), INTERVAL 7 DAY) THEN 1 ELSE 0 END), 0) AS overdue_receive_count,",
            "  COALESCE(SUM(CASE WHEN status = 1 AND pay_time < DATE_SUB(NOW(), INTERVAL 24 HOUR) THEN pay_amount ELSE 0 END), 0) AS overdue_delivery_amount,",
            "  COALESCE(SUM(CASE WHEN status = 2 AND delivery_time < DATE_SUB(NOW(), INTERVAL 7 DAY) THEN pay_amount ELSE 0 END), 0) AS overdue_receive_amount",
            "FROM oms_order",
            "WHERE deleted = 0"
    })
    AdminLogisticsSummaryVO selectLogisticsSummary();
}
