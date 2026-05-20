package com.gj.mall.vo;

import lombok.Data;

@Data
public class UploadStorageFileVO {

    private String url;

    private String relativePath;

    private Long size;

    private String lastModified;

    private Boolean cleanupCandidate;
}
