package com.grad.book.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 静态资源映射：/uploads/** 指向项目根目录下的 uploads/。
 * 不指向用户主目录，换机器、换账号仍可访问；种子图片与上传文件共用同一目录。
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.upload-dir:./uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absolute = Paths.get(uploadDir).toAbsolutePath().normalize().toUri().toString();
        // Path.toUri() 只有在目录「已存在」时才会补尾斜杠；
        // 若服务启动时 uploads/ 尚未创建，会得到不带斜杠的 file: URI，
        // Spring 会当成单个文件处理，导致所有 /uploads/** 请求 404。这里强制补齐。
        if (!absolute.endsWith("/")) {
            absolute = absolute + "/";
        }
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(absolute)
                .setCachePeriod(3600);
    }
}
