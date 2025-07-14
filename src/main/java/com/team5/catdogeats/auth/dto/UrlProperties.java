package com.team5.catdogeats.auth.dto;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class UrlProperties {

    @Value("${url.success}")
    private String successUrl;

    @Value("${url.fail}")
    private String failUrl;

    @Value("${url.logout}")
    private String logoutUrl;

    @Value("${url.duplicate}")
    private String duplicateUrl;

    @Value("${url.withdrawn}")
    private String withdrawnUrl;

    @Value("${url.login}")
    private String loginUrl;

    @Value("${url.role-selection}")
    private String roleSelectionUrl;
}
