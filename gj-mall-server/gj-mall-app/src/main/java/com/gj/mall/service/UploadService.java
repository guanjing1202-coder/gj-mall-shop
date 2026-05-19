package com.gj.mall.service;

import com.gj.mall.vo.UploadFileVO;
import org.springframework.web.multipart.MultipartFile;

public interface UploadService {

    UploadFileVO uploadImage(MultipartFile file, String scene);
}
