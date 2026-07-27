package com.gj.mall.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.gj.mall.common.exception.BizException;
import com.gj.mall.user.dto.AddressDTO;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.mapper.UmsUserAddressMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserAddressServiceImplTest {

    @Test
    void saveMakesFirstAddressDefaultWhenDefaultFlagOmitted() {
        UmsUserAddressMapper mapper = mock(UmsUserAddressMapper.class);
        UserAddressServiceImpl service = new UserAddressServiceImpl(mapper);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(0L);

        service.save(1001L, address(null, null));

        ArgumentCaptor<UmsUserAddress> captor = ArgumentCaptor.forClass(UmsUserAddress.class);
        verify(mapper).insert(captor.capture());
        UmsUserAddress saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(1001L);
        assertThat(saved.getIsDefault()).isEqualTo(1);
    }

    @Test
    void saveNormalizesInvalidDefaultFlagAndDoesNotClearExistingDefault() {
        UmsUserAddressMapper mapper = mock(UmsUserAddressMapper.class);
        UserAddressServiceImpl service = new UserAddressServiceImpl(mapper);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(2L);

        service.save(1001L, address(null, 2));

        ArgumentCaptor<UmsUserAddress> captor = ArgumentCaptor.forClass(UmsUserAddress.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(0);
        verify(mapper, never()).update(any(UmsUserAddress.class), any(Wrapper.class));
    }

    @Test
    void saveClearsOldDefaultWhenNewAddressExplicitlyDefault() {
        UmsUserAddressMapper mapper = mock(UmsUserAddressMapper.class);
        UserAddressServiceImpl service = new UserAddressServiceImpl(mapper);
        when(mapper.selectCount(any(Wrapper.class))).thenReturn(3L);

        service.save(1001L, address(null, 1));

        ArgumentCaptor<UmsUserAddress> captor = ArgumentCaptor.forClass(UmsUserAddress.class);
        verify(mapper).insert(captor.capture());
        assertThat(captor.getValue().getIsDefault()).isEqualTo(1);
        verify(mapper).update(any(UmsUserAddress.class), any(Wrapper.class));
    }

    @Test
    void getOneRejectsAddressOwnedByAnotherUser() {
        UmsUserAddressMapper mapper = mock(UmsUserAddressMapper.class);
        UserAddressServiceImpl service = new UserAddressServiceImpl(mapper);
        UmsUserAddress otherUserAddress = new UmsUserAddress();
        otherUserAddress.setId(10L);
        otherUserAddress.setUserId(2002L);
        when(mapper.selectById(10L)).thenReturn(otherUserAddress);

        assertThatThrownBy(() -> service.getOne(1001L, 10L))
                .isInstanceOf(BizException.class);
    }

    private static AddressDTO address(Long id, Integer isDefault) {
        AddressDTO dto = new AddressDTO();
        dto.setId(id);
        dto.setReceiver("张三");
        dto.setPhone("13800000000");
        dto.setProvince("浙江省");
        dto.setCity("杭州市");
        dto.setDistrict("西湖区");
        dto.setDetail("测试地址");
        dto.setPostCode("310000");
        dto.setIsDefault(isDefault);
        return dto;
    }
}
