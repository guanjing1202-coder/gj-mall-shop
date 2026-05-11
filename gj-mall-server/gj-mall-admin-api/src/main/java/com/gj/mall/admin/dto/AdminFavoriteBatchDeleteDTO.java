package com.gj.mall.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
@Schema(description = "批量删除会员收藏")
public class AdminFavoriteBatchDeleteDTO {

    @NotEmpty(message = "请选择收藏记录")
    private List<Long> ids;
}
