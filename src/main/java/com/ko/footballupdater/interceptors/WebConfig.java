package com.ko.footballupdater.interceptors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;

@Component
public class WebConfig implements WebMvcConfigurer {
    @Autowired
    ValidateHeaderSecretInterceptor validateHeaderSecretInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(validateHeaderSecretInterceptor).addPathPatterns("/api/**");
    }
}