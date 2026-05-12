package com.gj.mall.order.service;

import com.gj.mall.common.result.PageResult;
import com.gj.mall.order.dto.AdminOrderDeliverDTO;
import com.gj.mall.order.dto.CreateOrderDTO;
import com.gj.mall.order.dto.OrderQueryDTO;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.vo.AdminOrderFulfillmentSummaryVO;
import com.gj.mall.order.vo.OrderLogisticsVO;
import com.gj.mall.order.vo.OrderVO;

public interface OrderService {

    /** 创建订单：从购物车选中项下单。返回 orderNo */
    String create(Long userId, CreateOrderDTO dto);

    /** 取消订单（用户主动） */
    void cancel(Long userId, Long orderId);

    /** 超时取消（MQ 消费触发） */
    void timeoutCancel(Long orderId);

    /** 用户分页查询自己的订单 */
    PageResult<OrderVO> page(Long userId, OrderQueryDTO query);

    /** 后台分页查询全部订单 */
    PageResult<OrderVO> adminPage(OrderQueryDTO query);

    /** 后台订单履约汇总 */
    AdminOrderFulfillmentSummaryVO adminFulfillmentSummary();

    /** 用户查看自己的订单详情 */
    OrderVO detail(Long userId, Long orderId);

    /** 用户按订单号查看自己的订单详情 */
    OrderVO detailByOrderNo(Long userId, String orderNo);

    /** 用户查看自己的物流轨迹 */
    OrderLogisticsVO logistics(Long userId, Long orderId);

    /** 内部：根据订单号 / ID 拿主表（不带 items），跨模块用 */
    OmsOrder getByIdOrThrow(Long orderId);

    /** 内部：标记订单已支付（pay 模块回调）*/
    void markPaid(Long orderId, Integer payType);

    /** 后台发货 */
    OrderVO deliver(Long orderId, AdminOrderDeliverDTO dto);

    /** 用户确认收货 */
    void confirmReceive(Long userId, Long orderId);

    /**
     * 秒杀下单（Lua 已扣 Redis 库存，直接建订单）
     * @param userId     买家 ID
     * @param seckillSku 从 SeckillServiceImpl.lockStock 返回的 DB 快照
     * @param addressId  收货地址 ID
     * @return orderNo
     */
    String createSeckillOrder(Long userId,
                              com.gj.mall.marketing.entity.SmsSeckillSku seckillSku,
                              Long addressId);
}
