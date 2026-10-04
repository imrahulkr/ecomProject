package com.ecommerce.project.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    // Must be the same directory LocalDiskFileService writes uploads to, or uploaded images 404.
    @Value("${project.image}")
    private String imageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String dir = imageDir.endsWith("/") ? imageDir : imageDir + "/";
        registry.addResourceHandler("/images/**").addResourceLocations("file:" + dir);
    }
}
