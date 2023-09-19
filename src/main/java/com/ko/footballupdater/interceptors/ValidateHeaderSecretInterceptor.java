package com.ko.footballupdater.interceptors;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ValidateHeaderSecretInterceptor implements HandlerInterceptor {

    private final String AUTH_SECRET_HEADER_NAME = "Auth-Secret";

    @NotNull
    @Value("${endpoint.secret}")
    private String authSecret;

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String authSecretHeader = request.getHeader(AUTH_SECRET_HEADER_NAME);
        if (!authSecret.equals(authSecretHeader)) {
            throw new Exception("Secret doesn't match auth endpoint secret");
        }
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }
}
