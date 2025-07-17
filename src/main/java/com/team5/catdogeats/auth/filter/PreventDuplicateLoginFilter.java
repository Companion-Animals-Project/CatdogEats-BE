package com.team5.catdogeats.auth.filter;

import com.team5.catdogeats.auth.dto.UrlProperties;
import com.team5.catdogeats.auth.util.JwtUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreventDuplicateLoginFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UrlProperties urlProperties;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return !uri.startsWith("/v1/admin/")
                || !uri.startsWith("/oauth2/authorization/")
                || !uri.startsWith("/login/oauth2/code/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = jwtUtils.extractToken(request);
            // 토큰이 없으면 검사할 필요 없이 바로 다음 필터로
            if (!StringUtils.hasText(token)) {
                filterChain.doFilter(request, response);
                return;
            }

            String requestURI = request.getRequestURI();

                // 이미 에러 파라미터가 있으면 처리 건너뛰기
                if (request.getParameter("error") != null) {
                    filterChain.doFilter(request, response);
                    return;
                }

                // 토큰 검증 시도
                try {
                    boolean isValid = jwtUtils.validateToken(token);
                    if (isValid) {
                        log.warn("Already authenticated user tried to access OAuth2 login: {}", requestURI);
                        String duplicateUrl = urlProperties.getDuplicateUrl();
                        response.sendRedirect(duplicateUrl + "?error=already_authenticated");
                        return;
                    }
                } catch (Exception e) {
                    // 토큰 검증 중 예외 발생 시 로그만 남기고 진행
                    log.error("Token validation error: {}", e.getMessage());
                }

            // 필터 체인 계속 진행
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            // 모든 예외 로깅 후 필터 체인 계속 진행
            log.error("Error in PreventDuplicateLoginFilter: {}", e.getMessage(), e);
            filterChain.doFilter(request, response);
        }
    }
}
