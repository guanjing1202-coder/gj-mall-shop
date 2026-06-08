package com.gj.mall.order.service.impl;

import com.gj.mall.cart.service.CartService;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.marketing.service.CouponService;
import com.gj.mall.marketing.service.impl.SeckillServiceImpl;
import com.gj.mall.marketing.vo.CouponCheckResult;
import com.gj.mall.order.dto.CreateOrderDTO;
import com.gj.mall.order.dto.CreateOrderItemDTO;
import com.gj.mall.order.entity.OmsOrder;
import com.gj.mall.order.entity.OmsOrderItem;
import com.gj.mall.order.enums.OrderStatus;
import com.gj.mall.order.mapper.OmsOrderItemMapper;
import com.gj.mall.order.mapper.OmsOrderMapper;
import com.gj.mall.order.mq.OrderTimeoutProducer;
import com.gj.mall.order.service.FreightService;
import com.gj.mall.order.vo.FreightQuoteVO;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.service.UserAddressService;
import com.gj.mall.user.service.UserMessageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplCreateTest {

    private static final Long USER_ID = 7L;
    private static final Long ADDRESS_ID = 3L;

    @Mock
    private OmsOrderMapper orderMapper;
    @Mock
    private OmsOrderItemMapper itemMapper;
    @Mock
    private CartService cartService;
    @Mock
    private SkuService skuService;
    @Mock
    private UserAddressService addressService;
    @Mock
    private OrderTimeoutProducer timeoutProducer;
    @Mock
    private CouponService couponService;
    @Mock
    private PmsSpuMapper spuMapper;
    @Mock
    private UserMessageService messageService;
    @Mock
    private FreightService freightService;
    @Mock
    private SeckillServiceImpl seckillService;

    private OrderServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new OrderServiceImpl(orderMapper, itemMapper, cartService, skuService, addressService,
                timeoutProducer, couponService, spuMapper, messageService, freightService, seckillService);
    }

    @Test
    void directBuyMergesDuplicateSkuBeforeLockingStockAndCreatingItems() {
        stubCommonCheckout("65.00", "5.00", BigDecimal.ZERO, null);
        PmsSku sku = sku(100L, 200L, "重复SKU", "sku.png", "13.00", 20);
        when(skuService.getByIdOrThrow(100L)).thenReturn(sku);
        when(spuMapper.selectById(200L)).thenReturn(spu(200L, "商品", 1, "spu.png"));
        when(skuService.lockStock(100L, 5)).thenReturn(true);

        String orderNo = service.create(USER_ID, directOrder(dtoItem(100L, 2), dtoItem(100L, 3)));

        assertThat(orderNo).isNotBlank();
        verify(skuService).lockStock(100L, 5);
        ArgumentCaptor<OmsOrder> orderCaptor = ArgumentCaptor.forClass(OmsOrder.class);
        verify(orderMapper).insert(orderCaptor.capture());
        assertThat(orderCaptor.getValue().getTotalAmount()).isEqualByComparingTo("65.00");
        assertThat(orderCaptor.getValue().getPayAmount()).isEqualByComparingTo("70.00");
        assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.PENDING_PAY.getCode());

        ArgumentCaptor<OmsOrderItem> itemCaptor = ArgumentCaptor.forClass(OmsOrderItem.class);
        verify(itemMapper).insert(itemCaptor.capture());
        OmsOrderItem item = itemCaptor.getValue();
        assertThat(item.getSkuId()).isEqualTo(100L);
        assertThat(item.getQuantity()).isEqualTo(5);
        assertThat(item.getTotalAmount()).isEqualByComparingTo("65.00");
    }

    @Test
    void createReleasesActualLockedQuantityWhenLaterSkuLockFails() {
        PmsSku firstSku = sku(100L, 200L, "第一件", "first.png", "10.00", 20);
        PmsSku secondSku = sku(101L, 201L, "第二件", "second.png", "20.00", 20);
        when(skuService.getByIdOrThrow(100L)).thenReturn(firstSku);
        when(skuService.getByIdOrThrow(101L)).thenReturn(secondSku);
        when(spuMapper.selectById(200L)).thenReturn(spu(200L, "商品1", 1, "spu1.png"));
        when(spuMapper.selectById(201L)).thenReturn(spu(201L, "商品2", 1, "spu2.png"));
        when(addressService.getOne(USER_ID, ADDRESS_ID)).thenReturn(address());
        when(skuService.lockStock(100L, 5)).thenReturn(true);
        when(skuService.lockStock(101L, 1)).thenReturn(false);

        assertThatThrownBy(() -> service.create(USER_ID,
                directOrder(dtoItem(100L, 2), dtoItem(100L, 3), dtoItem(101L, 1))))
                .isInstanceOf(BizException.class);

        verify(skuService).releaseStock(100L, 5);
        verify(orderMapper, never()).insert(any(OmsOrder.class));
        verify(itemMapper, never()).insert(any(OmsOrderItem.class));
    }

    private void stubCommonCheckout(String orderAmount, String freightAmount,
                                    BigDecimal discountAmount, Long couponUserId) {
        when(addressService.getOne(USER_ID, ADDRESS_ID)).thenReturn(address());
        when(freightService.calculate(new BigDecimal(orderAmount), address())).thenReturn(freight(freightAmount));
        CouponCheckResult coupon = new CouponCheckResult();
        coupon.setDiscountAmount(discountAmount);
        coupon.setCouponUserId(couponUserId);
        when(couponService.check(eq(USER_ID), eq(null), eq(new BigDecimal(orderAmount)))).thenReturn(coupon);
        doAnswer(invocation -> {
            OmsOrder order = invocation.getArgument(0);
            order.setId(99L);
            return 1;
        }).when(orderMapper).insert(any(OmsOrder.class));
    }

    private static CreateOrderDTO directOrder(CreateOrderItemDTO... items) {
        CreateOrderDTO dto = new CreateOrderDTO();
        dto.setAddressId(ADDRESS_ID);
        dto.setItems(Arrays.asList(items));
        return dto;
    }

    private static CreateOrderItemDTO dtoItem(Long skuId, Integer quantity) {
        CreateOrderItemDTO dto = new CreateOrderItemDTO();
        dto.setSkuId(skuId);
        dto.setQuantity(quantity);
        return dto;
    }

    private static UmsUserAddress address() {
        UmsUserAddress address = new UmsUserAddress();
        address.setId(ADDRESS_ID);
        address.setUserId(USER_ID);
        address.setReceiver("张三");
        address.setPhone("13800000000");
        address.setProvince("浙江省");
        address.setCity("杭州市");
        address.setDistrict("西湖区");
        address.setDetail("测试地址");
        return address;
    }

    private static FreightQuoteVO freight(String amount) {
        FreightQuoteVO quote = new FreightQuoteVO();
        quote.setFreightAmount(new BigDecimal(amount));
        return quote;
    }

    private static PmsSku sku(Long id, Long spuId, String name, String image, String price, Integer stock) {
        PmsSku sku = new PmsSku();
        sku.setId(id);
        sku.setSpuId(spuId);
        sku.setName(name);
        sku.setImage(image);
        sku.setPrice(new BigDecimal(price));
        sku.setStock(stock);
        sku.setSpecData("{\"颜色\":\"黑色\"}");
        return sku;
    }

    private static PmsSpu spu(Long id, String name, Integer publishStatus, String mainImage) {
        PmsSpu spu = new PmsSpu();
        spu.setId(id);
        spu.setName(name);
        spu.setPublishStatus(publishStatus);
        spu.setMainImage(mainImage);
        return spu;
    }
}
