package com.gj.mall.admin.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class AdminCommentReplyDTO {

    @NotBlank(message = "回复内容不能为空")
    private String replyContent;
}
