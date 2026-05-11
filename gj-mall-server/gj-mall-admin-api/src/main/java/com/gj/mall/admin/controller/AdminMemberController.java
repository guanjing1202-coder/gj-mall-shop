package com.gj.mall.admin.controller;

import com.gj.mall.admin.dto.MemberQueryDTO;
import com.gj.mall.admin.service.AdminMemberService;
import com.gj.mall.admin.vo.MemberDetailVO;
import com.gj.mall.common.result.PageResult;
import com.gj.mall.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "后台会员", description = "C 端会员管理")
@RestController
@RequestMapping("/api/admin/member")
@RequiredArgsConstructor
public class AdminMemberController {

    private final AdminMemberService memberService;

    @Operation(summary = "会员分页")
    @GetMapping("/page")
    public Result<PageResult<MemberDetailVO>> page(MemberQueryDTO query) {
        return Result.success(memberService.page(query));
    }

    @Operation(summary = "会员详情")
    @GetMapping("/{id}")
    public Result<MemberDetailVO> detail(@PathVariable Long id) {
        return Result.success(memberService.detail(id));
    }

    @Operation(summary = "启停会员")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        memberService.updateStatus(id, status);
        return Result.success();
    }

    @Operation(summary = "删除会员")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        memberService.delete(id);
        return Result.success();
    }
}
