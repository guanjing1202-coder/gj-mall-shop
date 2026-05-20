package com.gj.mall.vo;

import lombok.Data;

import java.util.List;

@Data
public class UploadStorageSummaryVO {

    private String rootPath;

    private Integer retainDays;

    private Long totalFiles;

    private Long totalSize;

    private Long referencedFiles;

    private Long orphanFiles;

    private Long orphanSize;

    private Long cleanupCandidates;

    private Long cleanupCandidateSize;

    private Integer referenceCount;

    private List<UploadStorageFileVO> recentOrphans;
}
