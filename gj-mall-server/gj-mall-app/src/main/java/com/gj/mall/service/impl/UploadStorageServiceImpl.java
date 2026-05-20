package com.gj.mall.service.impl;

import com.gj.mall.common.exception.BizException;
import com.gj.mall.config.UploadProperties;
import com.gj.mall.service.UploadStorageService;
import com.gj.mall.vo.UploadCleanupResultVO;
import com.gj.mall.vo.UploadStorageFileVO;
import com.gj.mall.vo.UploadStorageSummaryVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class UploadStorageServiceImpl implements UploadStorageService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Pattern UPLOAD_URL_PATTERN = Pattern.compile("/uploads/[^\\s\"'\\\\\\])},，。；;]+");
    private static final int DEFAULT_RETAIN_DAYS = 7;
    private static final int MAX_RETAIN_DAYS = 3650;

    private final UploadProperties properties;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public UploadStorageSummaryVO summary(Integer retainDays) {
        int days = normalizeRetainDays(retainDays);
        List<UploadStorageFileVO> files = scanLocalFiles(days);
        Set<String> references = loadReferencedUrls();
        List<UploadStorageFileVO> orphans = files.stream()
                .filter(file -> !references.contains(file.getUrl()))
                .collect(Collectors.toList());
        List<UploadStorageFileVO> candidates = orphans.stream()
                .filter(file -> Boolean.TRUE.equals(file.getCleanupCandidate()))
                .collect(Collectors.toList());

        UploadStorageSummaryVO vo = new UploadStorageSummaryVO();
        vo.setRootPath(uploadRoot().toString());
        vo.setRetainDays(days);
        vo.setTotalFiles((long) files.size());
        vo.setTotalSize(sumSize(files));
        vo.setReferencedFiles((long) (files.size() - orphans.size()));
        vo.setOrphanFiles((long) orphans.size());
        vo.setOrphanSize(sumSize(orphans));
        vo.setCleanupCandidates((long) candidates.size());
        vo.setCleanupCandidateSize(sumSize(candidates));
        vo.setReferenceCount(references.size());
        vo.setRecentOrphans(orphans.stream()
                .sorted(Comparator.comparing(UploadStorageFileVO::getLastModified, Comparator.nullsLast(String::compareTo)).reversed())
                .limit(8)
                .collect(Collectors.toList()));
        return vo;
    }

    @Override
    public UploadCleanupResultVO cleanup(Integer retainDays, Boolean dryRun) {
        int days = normalizeRetainDays(retainDays);
        boolean previewOnly = dryRun == null || dryRun;
        Set<String> references = loadReferencedUrls();
        List<UploadStorageFileVO> files = scanLocalFiles(days);
        List<UploadStorageFileVO> candidates = files.stream()
                .filter(file -> Boolean.TRUE.equals(file.getCleanupCandidate()))
                .filter(file -> !references.contains(file.getUrl()))
                .collect(Collectors.toList());

        long deleted = 0L;
        long deletedSize = 0L;
        if (!previewOnly) {
            for (UploadStorageFileVO file : candidates) {
                Path target = uploadRoot().resolve(file.getRelativePath()).normalize();
                if (!target.startsWith(uploadRoot())) {
                    continue;
                }
                try {
                    long size = Files.size(target);
                    if (Files.deleteIfExists(target)) {
                        deleted++;
                        deletedSize += size;
                    }
                } catch (IOException ignored) {
                    // Ignore a single file failure so the cleanup can continue.
                }
            }
        }

        UploadCleanupResultVO vo = new UploadCleanupResultVO();
        vo.setRetainDays(days);
        vo.setDryRun(previewOnly);
        vo.setScannedFiles((long) files.size());
        vo.setCandidateFiles((long) candidates.size());
        vo.setCandidateSize(sumSize(candidates));
        vo.setDeletedFiles(deleted);
        vo.setDeletedSize(deletedSize);
        vo.setFiles(candidates.stream().limit(50).collect(Collectors.toList()));
        return vo;
    }

    private List<UploadStorageFileVO> scanLocalFiles(int retainDays) {
        Path root = uploadRoot();
        if (!Files.exists(root)) {
            return new ArrayList<>();
        }
        Instant threshold = Instant.now().minusSeconds((long) retainDays * 24 * 60 * 60);
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> toFileVO(root, path, threshold))
                    .filter(file -> file != null)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new BizException("上传目录扫描失败");
        }
    }

    private UploadStorageFileVO toFileVO(Path root, Path path, Instant threshold) {
        try {
            Path relative = root.relativize(path);
            FileTime lastModified = Files.getLastModifiedTime(path);
            UploadStorageFileVO vo = new UploadStorageFileVO();
            vo.setRelativePath(relative.toString().replace("\\", "/"));
            vo.setUrl(joinUrl(vo.getRelativePath()));
            vo.setSize(Files.size(path));
            vo.setLastModified(formatTime(lastModified.toInstant()));
            vo.setCleanupCandidate(lastModified.toInstant().isBefore(threshold));
            return vo;
        } catch (IOException e) {
            return null;
        }
    }

    private Set<String> loadReferencedUrls() {
        Set<String> result = new HashSet<>();
        List<TextColumn> columns = loadTextColumns();
        for (TextColumn column : columns) {
            String sql = "SELECT `" + column.columnName + "` FROM `" + column.tableName + "` WHERE `" + column.columnName + "` LIKE ?";
            try {
                jdbcTemplate.query(sql, rs -> {
                    String value = rs.getString(1);
                    extractUploadUrls(value, result);
                }, "%" + normalizedPrefix() + "%");
            } catch (Exception ignored) {
                // Some text columns may be inaccessible due to schema changes; skip them for best-effort diagnostics.
            }
        }
        return result;
    }

    private List<TextColumn> loadTextColumns() {
        String sql = "SELECT TABLE_NAME, COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS " +
                "WHERE TABLE_SCHEMA = DATABASE() AND DATA_TYPE IN " +
                "('char','varchar','tinytext','text','mediumtext','longtext','json')";
        return jdbcTemplate.query(sql, (rs, rowNum) -> new TextColumn(rs.getString(1), rs.getString(2)));
    }

    private void extractUploadUrls(String value, Set<String> output) {
        if (!StringUtils.hasText(value)) {
            return;
        }
        Matcher matcher = UPLOAD_URL_PATTERN.matcher(value);
        while (matcher.find()) {
            output.add(normalizeUrl(matcher.group()));
        }
    }

    private String normalizeUrl(String url) {
        String text = url == null ? "" : url.trim();
        int queryIndex = text.indexOf('?');
        if (queryIndex >= 0) {
            text = text.substring(0, queryIndex);
        }
        try {
            text = URLDecoder.decode(text, StandardCharsets.UTF_8.name());
        } catch (Exception ignored) {
        }
        String prefix = normalizedPrefix();
        if (!text.startsWith(prefix)) {
            return text;
        }
        return prefix + text.substring(prefix.length()).replace("\\", "/");
    }

    private String joinUrl(String relativePath) {
        String prefix = normalizedPrefix();
        String path = relativePath == null ? "" : relativePath.replace("\\", "/");
        while (path.startsWith("/")) {
            path = path.substring(1);
        }
        return prefix + "/" + path;
    }

    private String normalizedPrefix() {
        String prefix = StringUtils.hasText(properties.getUrlPrefix()) ? properties.getUrlPrefix().trim() : "/uploads";
        if (!prefix.startsWith("/")) {
            prefix = "/" + prefix;
        }
        while (prefix.endsWith("/") && prefix.length() > 1) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }
        return prefix;
    }

    private Path uploadRoot() {
        return Paths.get(properties.getPath()).toAbsolutePath().normalize();
    }

    private int normalizeRetainDays(Integer retainDays) {
        if (retainDays == null) {
            return DEFAULT_RETAIN_DAYS;
        }
        return Math.max(0, Math.min(MAX_RETAIN_DAYS, retainDays));
    }

    private Long sumSize(List<UploadStorageFileVO> files) {
        return files.stream().mapToLong(file -> file.getSize() == null ? 0L : file.getSize()).sum();
    }

    private String formatTime(Instant instant) {
        return TIME_FORMAT.format(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
    }

    private static class TextColumn {
        private final String tableName;
        private final String columnName;

        private TextColumn(String tableName, String columnName) {
            this.tableName = tableName;
            this.columnName = columnName;
        }
    }
}
