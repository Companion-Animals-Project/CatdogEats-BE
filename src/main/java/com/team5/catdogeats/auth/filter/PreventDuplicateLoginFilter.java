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
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestURI = request.getRequestURI();

        if (requestURI.startsWith("/oauth2/authorization/") ||
                requestURI.startsWith("/login/oauth2/code/")) {
            String duplicateUrl = urlProperties.getDuplicateUrl();
            String url = duplicateUrl + "/?error=already_authenticated";
            String token = jwtUtils.extractToken(request);
            if (StringUtils.hasText(token) && jwtUtils.validateToken(token)) {
                log.debug("Duplicate URL: {}", duplicateUrl);
                log.warn("Already authenticated user tried to access OAuth2 login: {}", request.getRequestURI());
                if (request.getParameter("error") == null) {
                    response.sendRedirect(url + "?error=already_authenticated");
                    return;
                }
            }

        }
        filterChain.doFilter(request, response);
    }
}
