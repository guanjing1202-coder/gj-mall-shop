package com.gj.mall.user.service;

import com.gj.mall.user.dto.AddressDTO;
import com.gj.mall.user.entity.UmsUserAddress;

import java.util.List;

public interface UserAddressService {

    List<UmsUserAddress> list(Long userId);

    UmsUserAddress getOne(Long userId, Long id);

    Long save(Long userId, AddressDTO dto);

    void update(Long userId, AddressDTO dto);

    void delete(Long userId, Long id);

    void setDefault(Long userId, Long id);
}
