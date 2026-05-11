package com.gj.mall.user.controller;

import com.gj.mall.common.result.Result;
import com.gj.mall.framework.context.UserContext;
import com.gj.mall.user.dto.AddressDTO;
import com.gj.mall.user.entity.UmsUserAddress;
import com.gj.mall.user.service.UserAddressService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;

@Tag(name = "3-收货地址", description = "用户收货地址 CRUD")
@RestController
@RequestMapping("/api/user/address")
@RequiredArgsConstructor
public class AddressController {

    private final UserAddressService addressService;

    @Operation(summary = "我的地址列表")
    @GetMapping
    public Result<List<UmsUserAddress>> list() {
        return Result.success(addressService.list(UserContext.getUserId()));
    }

    @Operation(summary = "新增地址")
    @PostMapping
    public Result<Long> add(@Valid @RequestBody AddressDTO dto) {
        return Result.success(addressService.save(UserContext.getUserId(), dto));
    }

    @Operation(summary = "修改地址")
    @PutMapping
    public Result<Void> update(@Valid @RequestBody AddressDTO dto) {
        addressService.update(UserContext.getUserId(), dto);
        return Result.success();
    }

    @Operation(summary = "删除地址")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        addressService.delete(UserContext.getUserId(), id);
        return Result.success();
    }

    @Operation(summary = "设为默认")
    @PutMapping("/{id}/default")
    public Result<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(UserContext.getUserId(), id);
        return Result.success();
    }
}
