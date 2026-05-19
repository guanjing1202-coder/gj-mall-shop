package com.gj.mall.marketing.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.gj.mall.marketing.dto.CouponCreateDTO;
import com.gj.mall.marketing.entity.SmsCoupon;
import com.gj.mall.marketing.vo.CouponCheckResult;
import com.gj.mall.marketing.vo.CouponCenterVO;
import com.gj.mall.marketing.vo.CouponVO;
import com.gj.mall.marketing.vo.MyCouponVO;

import java.math.BigDecimal;
import java.util.List;

public interface CouponService {

    // -------- C 端 --------
    /** 领券（防重、限量、分布式锁） */
    void receive(Long userId, Long couponId);

    /** 我的优惠券列表（status: null=全部 0未用 1已用 2过期） */
    List<MyCouponVO> myList(Long userId, Integer status);

    /** 可用优惠券列表（适用于当前订单金额） */
    List<MyCouponVO> available(Long userId, BigDecimal orderAmount);

    /** 结算页优惠券候选列表：包含可用券和不可用原因 */
    List<MyCouponVO> checkoutList(Long userId, BigDecimal orderAmount);

    /** 领券中心：上架、有效、仍有余量的优惠券 */
    List<CouponCenterVO> centerList();

    /**
     * 校验并计算折扣（下单时调用）
     * @return 折扣结果（含 couponUserId + discountAmount），若 couponId=null 返回 discountAmount=0
     */
    CouponCheckResult check(Long userId, Long couponId, BigDecimal orderAmount);

    /** 核销（支付成功时调用，传 coupon_user.id） */
    void use(Long couponUserId, Long orderId);

    /** 释放（订单取消时归还，传 coupon_user.id） */
    void release(Long couponUserId);

    // -------- Admin 端 --------
    Page<CouponVO> adminPage(long pageNum, long pageSize, Integer status);

    Long adminCreate(CouponCreateDTO dto);

    void adminSetStatus(Long id, Integer status);

    SmsCoupon getByIdOrThrow(Long id);
}
