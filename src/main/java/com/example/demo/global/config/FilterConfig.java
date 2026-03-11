package com.example.demo.global.config;

import com.example.demo.auth.application.JwtAuthFilter;
import com.example.demo.auth.application.JwtTokenProvider;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

// JwtAuthFilter 등록
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<JwtAuthFilter> jwtAuthFilter(JwtTokenProvider jwtTokenProvider) {
        FilterRegistrationBean<JwtAuthFilter> reg = new FilterRegistrationBean<>();
        reg.setFilter(new JwtAuthFilter(jwtTokenProvider));
        reg.setOrder(1); // 가능한 앞에서 실행
        reg.addUrlPatterns("/*");
        return reg;
    }
}
