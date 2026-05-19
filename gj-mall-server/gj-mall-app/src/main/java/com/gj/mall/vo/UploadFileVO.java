package com.gj.mall.vo;

import lombok.Data;

@Data
public class UploadFileVO {

    private String url;

    private String fileName;

    private String originalName;

    private String contentType;

    private Long size;
}
