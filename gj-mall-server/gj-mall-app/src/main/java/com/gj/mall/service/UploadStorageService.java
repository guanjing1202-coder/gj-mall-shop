package com.gj.mall.service;

import com.gj.mall.vo.UploadCleanupResultVO;
import com.gj.mall.vo.UploadStorageSummaryVO;

public interface UploadStorageService {

    UploadStorageSummaryVO summary(Integer retainDays);

    UploadCleanupResultVO cleanup(Integer retainDays, Boolean dryRun);
}
