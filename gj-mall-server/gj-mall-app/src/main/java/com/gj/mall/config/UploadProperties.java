package com.gj.mall.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Data
@Component
@ConfigurationProperties(prefix = "mall.upload")
public class UploadProperties {

    private String path = "uploads";

    private String urlPrefix = "/uploads";

    private long maxImageSize = 10 * 1024 * 1024L;

    private List<String> allowedImageTypes = new ArrayList<String>() {{
        add("image/jpeg");
        add("image/png");
        add("image/webp");
        add("image/gif");
    }};
}
