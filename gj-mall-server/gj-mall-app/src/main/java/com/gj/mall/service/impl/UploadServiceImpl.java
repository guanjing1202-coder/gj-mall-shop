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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
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
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() == null ? "image" : file.getOriginalFilename());
        ImageType detectedType = detectImageType(file);
        if (detectedType == null || !properties.getAllowedImageTypes().contains(detectedType.contentType)) {
            throw new BizException("图片内容不合法，请上传真实的 JPG、PNG、WEBP、GIF 图片");
        }
        String contentType = StringUtils.hasText(file.getContentType()) ? file.getContentType().toLowerCase(Locale.ROOT) : "";
        if (StringUtils.hasText(contentType)
                && !"application/octet-stream".equals(contentType)
                && !properties.getAllowedImageTypes().contains(contentType)) {
            throw new BizException("仅支持 JPG、PNG、WEBP、GIF 图片");
        }
        if (properties.getAllowedImageTypes().contains(contentType) && !contentType.equals(detectedType.contentType)) {
            throw new BizException("图片类型与文件内容不一致，请重新选择图片");
        }
        String extension = extensionOf(originalName, detectedType);
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
            vo.setContentType(detectedType.contentType);
            vo.setSize(file.getSize());
            return vo;
        } catch (IOException e) {
            throw new BizException("图片上传失败，请稍后再试");
        }
    }

    private ImageType detectImageType(MultipartFile file) {
        byte[] header = new byte[12];
        int length;
        try (InputStream input = file.getInputStream()) {
            length = input.read(header);
        } catch (IOException e) {
            throw new BizException("图片读取失败，请重新上传");
        }
        if (length < 4) {
            return null;
        }
        if (startsWith(header, length, new int[]{0xFF, 0xD8, 0xFF})) {
            return ImageType.JPG;
        }
        if (startsWith(header, length, new int[]{0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A})) {
            return ImageType.PNG;
        }
        if (startsWith(header, length, new int[]{0x47, 0x49, 0x46, 0x38})) {
            return ImageType.GIF;
        }
        if (length >= 12
                && startsWith(header, length, new int[]{0x52, 0x49, 0x46, 0x46})
                && Arrays.equals(Arrays.copyOfRange(header, 8, 12), new byte[]{0x57, 0x45, 0x42, 0x50})) {
            return ImageType.WEBP;
        }
        return null;
    }

    private boolean startsWith(byte[] header, int length, int[] signature) {
        if (length < signature.length) {
            return false;
        }
        for (int i = 0; i < signature.length; i++) {
            if ((header[i] & 0xFF) != signature[i]) {
                return false;
            }
        }
        return true;
    }

    private String extensionOf(String originalName, ImageType detectedType) {
        String extension = "";
        int index = originalName.lastIndexOf('.');
        if (index >= 0 && index < originalName.length() - 1) {
            extension = originalName.substring(index).toLowerCase(Locale.ROOT);
        }
        if (detectedType.extensions.contains(extension)) {
            return extension;
        }
        return detectedType.defaultExtension;
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

    private enum ImageType {
        JPG("image/jpeg", ".jpg", Arrays.asList(".jpg", ".jpeg")),
        PNG("image/png", ".png", Arrays.asList(".png")),
        WEBP("image/webp", ".webp", Arrays.asList(".webp")),
        GIF("image/gif", ".gif", Arrays.asList(".gif"));

        private final String contentType;
        private final String defaultExtension;
        private final java.util.List<String> extensions;

        ImageType(String contentType, String defaultExtension, java.util.List<String> extensions) {
            this.contentType = contentType;
            this.defaultExtension = defaultExtension;
            this.extensions = extensions;
        }
    }
}
