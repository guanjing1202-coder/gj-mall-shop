package com.gj.mall.user.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.gj.mall.common.enums.ResultCode;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.user.dto.AddressDTO;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.mapper.UmsUserAddressMapper;
import com.gj.mall.user.service.UserAddressService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserAddressServiceImpl implements UserAddressService {

    private final UmsUserAddressMapper mapper;

    @Override
    public List<UmsUserAddress> list(Long userId) {
        return mapper.selectList(Wrappers.<UmsUserAddress>lambdaQuery()
                .eq(UmsUserAddress::getUserId, userId)
                .orderByDesc(UmsUserAddress::getIsDefault)
                .orderByDesc(UmsUserAddress::getUpdateTime));
    }

    @Override
    public UmsUserAddress getOne(Long userId, Long id) {
        UmsUserAddress addr = mapper.selectById(id);
        if (addr == null || !addr.getUserId().equals(userId)) {
            throw new BizException(ResultCode.DATA_NOT_FOUND);
        }
        return addr;
    }

    @Override
    @Transactional
    public Long save(Long userId, AddressDTO dto) {
        UmsUserAddress addr = new UmsUserAddress();
        BeanUtil.copyProperties(dto, addr);
        addr.setId(null);
        addr.setUserId(userId);
        if (addr.getIsDefault() == null) addr.setIsDefault(0);
        if (addr.getIsDefault() == 1) {
            clearDefault(userId);
        }
        mapper.insert(addr);
        return addr.getId();
    }

    @Override
    @Transactional
    public void update(Long userId, AddressDTO dto) {
        if (dto.getId() == null) throw new BizException(ResultCode.PARAM_MISSING, "缺少地址ID");
        UmsUserAddress old = getOne(userId, dto.getId());
        UmsUserAddress addr = new UmsUserAddress();
        BeanUtil.copyProperties(dto, addr);
        addr.setUserId(userId);
        if (Integer.valueOf(1).equals(addr.getIsDefault()) && !Integer.valueOf(1).equals(old.getIsDefault())) {
            clearDefault(userId);
        }
        mapper.updateById(addr);
    }

    @Override
    public void delete(Long userId, Long id) {
        getOne(userId, id);
        mapper.deleteById(id);
    }

    @Override
    @Transactional
    public void setDefault(Long userId, Long id) {
        getOne(userId, id);
        clearDefault(userId);
        UmsUserAddress upd = new UmsUserAddress();
        upd.setId(id);
        upd.setIsDefault(1);
        mapper.updateById(upd);
    }

    private void clearDefault(Long userId) {
        UmsUserAddress upd = new UmsUserAddress();
        upd.setIsDefault(0);
        mapper.update(upd, Wrappers.<UmsUserAddress>lambdaUpdate()
                .eq(UmsUserAddress::getUserId, userId)
                .eq(UmsUserAddress::getIsDefault, 1));
    }
}
