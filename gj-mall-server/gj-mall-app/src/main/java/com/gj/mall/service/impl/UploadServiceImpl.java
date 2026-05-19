package com.gj.mall.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.config.UploadProperties;
import com.gj.mall.service.UploadService;
import com.gj.mall.vo.UploadFileVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UploadServiceImpl implements UploadService {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final UploadProperties properties;

    @Override
    public UploadFileVO uploadImage(MultipartFile file, String scene) {
        if (file == null || file.isEmpty()) {
            throw new BizException("请选择要上传的图片");
        }
        if (file.getSize() > properties.getMaxImageSize()) {
            throw new BizException("图片不能超过 " + readableSize(properties.getMaxImageSize()));
        }
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (!properties.getAllowedImageTypes().contains(contentType)) {
            throw new BizException("仅支持 JPG、PNG、WEBP、GIF 图片");
        }

        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        String extension = extensionOf(originalName, contentType);
        String safeScene = safeSegment(scene);
        String datePath = LocalDate.now().format(DATE_PATH);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;

        Path root = Paths.get(properties.getPath()).toAbsolutePath().normalize();
        Path targetDir = root.resolve(Paths.get(safeScene, datePath)).normalize();
        if (!targetDir.startsWith(root)) {
            throw new BizException("上传路径不合法");
        }
        try {
            Files.createDirectories(targetDir);
            Path target = targetDir.resolve(fileName).normalize();
            file.transferTo(target.toFile());

            UploadFileVO vo = new UploadFileVO();
            vo.setUrl(joinUrl(properties.getUrlPrefix(), safeScene, datePath, fileName));
            vo.setFileName(fileName);
            vo.setOriginalName(originalName);
            vo.setContentType(contentType);
            vo.setSize(file.getSize());
            return vo;
        } catch (IOException e) {
            throw new BizException("图片上传失败，请稍后再试");
        }
    }

    private String extensionOf(String originalName, String contentType) {
        String extension = "";
        int index = originalName.lastIndexOf('.');
        if (index >= 0 && index < originalName.length() - 1) {
            extension = originalName.substring(index).toLowerCase(Locale.ROOT);
        }
        if (extension.matches("\\.(jpg|jpeg|png|webp|gif)")) {
            return extension;
        }
        if ("image/png".equals(contentType)) {
            return ".png";
        }
        if ("image/webp".equals(contentType)) {
            return ".webp";
        }
        if ("image/gif".equals(contentType)) {
            return ".gif";
        }
        return ".jpg";
    }

    private String safeSegment(String scene) {
        String text = StringUtils.hasText(scene) ? scene.trim().toLowerCase(Locale.ROOT) : "common";
        text = text.replaceAll("[^a-z0-9_-]", "-");
        text = text.replaceAll("-+", "-");
        if (!StringUtils.hasText(text)) {
            return "common";
        }
        return text.length() > 32 ? text.substring(0, 32) : text;
    }

    private String joinUrl(String prefix, String scene, String datePath, String fileName) {
        String base = StringUtils.hasText(prefix) ? prefix.trim() : "/uploads";
        if (!base.startsWith("/")) {
            base = "/" + base;
        }
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/" + scene + "/" + datePath + "/" + fileName;
    }

    private String readableSize(long bytes) {
        long mb = bytes / 1024L / 1024L;
        return Math.max(1L, mb) + "MB";
    }
}
