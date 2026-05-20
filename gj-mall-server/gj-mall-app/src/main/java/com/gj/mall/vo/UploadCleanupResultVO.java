package com.gj.mall.vo;

import lombok.Data;

import java.util.List;

@Data
public class UploadCleanupResultVO {

    private Integer retainDays;

    private Boolean dryRun;

    private Long scannedFiles;

    private Long deletedFiles;

    private Long deletedSize;

    private Long candidateFiles;

    private Long candidateSize;

    private List<UploadStorageFileVO> files;
}
