package com.restaurant.app.core.common;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final String localPath;

    public WebConfig(@Value("${app.storage.local-path:./uploads/dishes/}") String localPath) {
        this.localPath = localPath;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path basePath = Paths.get(localPath).toAbsolutePath().normalize();
        Path uploadsDir = basePath;
        if (basePath.getFileName() != null && basePath.getFileName().toString().equals("dishes")) {
            Path parent = basePath.getParent();
            if (parent != null) {
                uploadsDir = parent;
            }
        }

        String location = uploadsDir.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(location)
                .setCacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable());
    }
}
