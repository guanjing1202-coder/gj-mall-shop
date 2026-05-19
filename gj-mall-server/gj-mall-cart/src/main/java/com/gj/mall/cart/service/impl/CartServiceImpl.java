package com.gj.mall.cart.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.alibaba.fastjson2.JSON;
import com.gj.mall.cart.dto.AddCartDTO;
import com.gj.mall.cart.dto.MergeCartDTO;
import com.gj.mall.cart.dto.UpdateCartDTO;
import com.gj.mall.cart.service.CartService;
import com.gj.mall.cart.vo.CartItemVO;
import com.gj.mall.cart.vo.CartVO;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.product.entity.PmsSku;
import com.gj.mall.product.entity.PmsSpu;
import com.gj.mall.product.mapper.PmsSpuMapper;
import com.gj.mall.product.service.SkuService;
import com.gj.mall.product.vo.SkuVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private static final String KEY_PREFIX = "mall:cart:";
    /** 单用户最多 200 个 SKU */
    private static final int MAX_ITEMS = 200;

    private final StringRedisTemplate redis;
    private final SkuService skuService;
    private final PmsSpuMapper spuMapper;

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }

    private HashOperations<String, String, String> hash() {
        return redis.opsForHash();
    }

    /** Redis 中存的最小结构 */
    private static class CartLine {
        public Long skuId;
        public Integer quantity;
        public Integer selected;
        public Long addTime;
    }

    @Override
    public CartVO get(Long userId) {
        Map<String, String> raw = hash().entries(key(userId));
        if (CollUtil.isEmpty(raw)) {
            return emptyCart();
        }
        List<CartLine> lines = raw.values().stream()
                .map(s -> JSON.parseObject(s, CartLine.class))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (lines.isEmpty()) return emptyCart();

        // 一次拿全部 SKU + 关联 SPU
        List<Long> skuIds = lines.stream().map(l -> l.skuId).collect(Collectors.toList());
        List<PmsSku> skus = skuService.listByIds(skuIds);
        Map<Long, PmsSku> skuMap = skus.stream().collect(Collectors.toMap(PmsSku::getId, s -> s));
        Set<Long> spuIds = skus.stream().map(PmsSku::getSpuId).collect(Collectors.toSet());
        Map<Long, PmsSpu> spuMap = spuIds.isEmpty() ? Collections.emptyMap()
                : spuMapper.selectBatchIds(spuIds).stream()
                        .collect(Collectors.toMap(PmsSpu::getId, s -> s));

        // 按加入时间倒序展示
        lines.sort(Comparator.comparing((CartLine l) -> l.addTime == null ? 0L : l.addTime).reversed());

        List<CartItemVO> items = new ArrayList<>(lines.size());
        int totalCount = 0, selectedCount = 0;
        BigDecimal selectedAmount = BigDecimal.ZERO;

        for (CartLine l : lines) {
            CartItemVO vo = new CartItemVO();
            vo.setSkuId(l.skuId);
            vo.setQuantity(l.quantity);
            vo.setSelected(l.selected == null ? 1 : l.selected);

            PmsSku sku = skuMap.get(l.skuId);
            if (sku == null) {
                // SKU 已删除
                vo.setInvalid(true);
                vo.setStockEnough(false);
                vo.setInvalidReason("商品已删除或下架");
                vo.setSkuName("商品已下架");
                vo.setPrice(BigDecimal.ZERO);
                vo.setTotalAmount(BigDecimal.ZERO);
                items.add(vo);
                totalCount += l.quantity == null ? 0 : l.quantity;
                continue;
            }
            SkuVO skuVO = skuService.toVO(sku);
            vo.setSpuId(sku.getSpuId());
            vo.setSkuName(sku.getName());
            vo.setImage(sku.getImage());
            vo.setPrice(sku.getPrice());
            vo.setStock(sku.getStock());
            vo.setSpecData(skuVO.getSpecData());

            PmsSpu spu = spuMap.get(sku.getSpuId());
            if (spu != null) {
                vo.setSpuName(spu.getName());
                vo.setPublishStatus(spu.getPublishStatus());
                if (vo.getImage() == null) vo.setImage(spu.getMainImage());
            }
            int quantity = l.quantity == null ? 0 : l.quantity;
            int stock = sku.getStock() == null ? 0 : sku.getStock();
            boolean offShelf = spu == null || !Integer.valueOf(1).equals(spu.getPublishStatus());
            boolean stockEnough = stock >= quantity && quantity > 0;
            boolean invalid = offShelf || !stockEnough;
            vo.setInvalid(invalid);
            vo.setStockEnough(stockEnough);
            if (offShelf) {
                vo.setInvalidReason("商品已下架");
            } else if (stock <= 0) {
                vo.setInvalidReason("商品暂时无库存");
            } else if (!stockEnough) {
                vo.setInvalidReason("库存仅剩 " + stock + " 件，请调整数量");
            }

            BigDecimal lineTotal = sku.getPrice().multiply(BigDecimal.valueOf(quantity));
            vo.setTotalAmount(lineTotal);

            totalCount += quantity;
            if (!invalid && Integer.valueOf(1).equals(vo.getSelected())) {
                selectedCount += quantity;
                selectedAmount = selectedAmount.add(lineTotal);
            }
            items.add(vo);
        }

        CartVO cart = new CartVO();
        cart.setItems(items);
        cart.setTotalCount(totalCount);
        cart.setSelectedCount(selectedCount);
        cart.setSelectedAmount(selectedAmount);
        return cart;
    }

    private CartVO emptyCart() {
        CartVO cart = new CartVO();
        cart.setItems(Collections.emptyList());
        cart.setTotalCount(0);
        cart.setSelectedCount(0);
        cart.setSelectedAmount(BigDecimal.ZERO);
        return cart;
    }

    @Override
    public void add(Long userId, AddCartDTO dto) {
        // 校验 SKU 上架且有库存
        PmsSku sku = skuService.getByIdOrThrow(dto.getSkuId());
        PmsSpu spu = spuMapper.selectById(sku.getSpuId());
        if (spu == null || !Integer.valueOf(1).equals(spu.getPublishStatus())) {
            throw new BizException(ResultCode.PRODUCT_OFF_SHELF);
        }
        int wantQty = dto.getQuantity();

        String k = key(userId);
        String field = dto.getSkuId().toString();
        String existing = hash().get(k, field);

        int finalQty = wantQty;
        long now = System.currentTimeMillis();
        Long addTime = now;
        Integer selected = 1;
        if (existing != null) {
            CartLine old = JSON.parseObject(existing, CartLine.class);
            if (old != null) {
                finalQty = (old.quantity == null ? 0 : old.quantity) + wantQty;
                addTime = old.addTime == null ? now : old.addTime;
                selected = old.selected == null ? 1 : old.selected;
            }
        } else {
            // 新增前判断容量
            Long size = hash().size(k);
            if (size != null && size >= MAX_ITEMS) {
                throw new BizException(ResultCode.OPERATION_FORBIDDEN, "购物车已满");
            }
        }
        if (sku.getStock() == null || finalQty > sku.getStock()) {
            throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
        }

        CartLine line = new CartLine();
        line.skuId = dto.getSkuId();
        line.quantity = finalQty;
        line.selected = selected;
        line.addTime = addTime;
        hash().put(k, field, JSON.toJSONString(line));
    }

    @Override
    public void update(Long userId, UpdateCartDTO dto) {
        String k = key(userId);
        String field = dto.getSkuId().toString();
        String existing = hash().get(k, field);
        if (existing == null) throw new BizException(ResultCode.DATA_NOT_FOUND, "购物车项不存在");
        CartLine line = JSON.parseObject(existing, CartLine.class);
        if (line == null) throw new BizException(ResultCode.DATA_NOT_FOUND);

        if (dto.getQuantity() != null) {
            PmsSku sku = skuService.getByIdOrThrow(dto.getSkuId());
            if (sku.getStock() == null || dto.getQuantity() > sku.getStock()) {
                throw new BizException(ResultCode.STOCK_NOT_ENOUGH);
            }
            line.quantity = dto.getQuantity();
        }
        if (dto.getSelected() != null) {
            line.selected = dto.getSelected() == 0 ? 0 : 1;
        }
        hash().put(k, field, JSON.toJSONString(line));
    }

    @Override
    public void remove(Long userId, Long skuId) {
        hash().delete(key(userId), skuId.toString());
    }

    @Override
    public void clear(Long userId) {
        redis.delete(key(userId));
    }

    @Override
    public void selectAll(Long userId, boolean selected) {
        String k = key(userId);
        Map<String, String> raw = hash().entries(k);
        if (CollUtil.isEmpty(raw)) return;
        Map<String, String> upd = new HashMap<>(raw.size());
        for (Map.Entry<String, String> e : raw.entrySet()) {
            CartLine line = JSON.parseObject(e.getValue(), CartLine.class);
            if (line == null) continue;
            line.selected = selected ? 1 : 0;
            upd.put(e.getKey(), JSON.toJSONString(line));
        }
        if (!upd.isEmpty()) hash().putAll(k, upd);
    }

    @Override
    public void merge(Long userId, MergeCartDTO dto) {
        for (AddCartDTO item : dto.getItems()) {
            try {
                add(userId, item);
            } catch (BizException ignored) {
                // 单项失败不影响其它合并
            }
        }
    }
}
