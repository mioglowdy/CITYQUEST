package com.cityquest.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(@NonNull InterceptorRegistry registry) {
        // 添加JWT拦截器，排除登录和注册接口
        // 注意：由于设置了context-path: /api，拦截器路径匹配基于context-path之后的路径
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/user/login", 
                    "/user/register",
                    "/uploads/**"  // 排除静态资源路径，允许公开访问图片
                );
    }

    @Override
    public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
        // 静态资源映射：/uploads/** 指向本地 uploads 目录
        // 使用绝对路径，确保能找到文件
        // 注意：user.dir 在 Spring Boot 运行时指向的是项目根目录（backend目录）
        String userDir = System.getProperty("user.dir");
        String uploadPath = userDir + java.io.File.separator + "uploads" + java.io.File.separator;
        
        // 打印路径信息用于调试
        System.out.println("========= 静态资源映射配置 =========");
        System.out.println("user.dir: " + userDir);
        System.out.println("uploadPath: " + uploadPath);
        System.out.println("映射: /uploads/** -> file:" + uploadPath);
        
        // 检查目录是否存在
        java.io.File uploadDir = new java.io.File(uploadPath);
        if (!uploadDir.exists()) {
            System.out.println("警告: uploads 目录不存在，尝试创建: " + uploadPath);
            if (uploadDir.mkdirs()) {
                System.out.println("成功创建 uploads 目录");
            } else {
                System.err.println("无法创建 uploads 目录: " + uploadPath);
            }
        } else {
            System.out.println("uploads 目录已存在: " + uploadPath);
        }
        
        registry.addResourceHandler("/uploads/**")
                .addResourceLocations("file:" + uploadPath)
                .setCachePeriod(3600); // 缓存1小时
        
        System.out.println("========= 静态资源映射配置完成 =========");
    }
}