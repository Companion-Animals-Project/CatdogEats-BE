package com.team5.catdogeats.auth.handler;

import com.team5.catdogeats.auth.dto.UrlProperties;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.global.config.CookieProperties;
import com.team5.catdogeats.users.domain.enums.Role;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final CookieUtils cookieUtils;
    private final JwtService jwtService;
    private final JwtUtils jwtUtils;
    private final CookieProperties cookieProperties;
    private final RefreshTokenService refreshTokenService;
    private final UrlProperties urlProperties;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws ServletException, IOException {

        try {
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.debug("OAuth2 인증 성공 후 세션 무효화: {}", session.getId());
                session.invalidate();
            }

        String token = jwtService.createAccessToken(authentication);
        String refreshTokenId = refreshTokenService.createRefreshToken(authentication);
        log.debug("Created refresh token: {}", refreshTokenId);
        String successUrl = urlProperties.getSuccessUrl();
        log.debug("Success URL: {}", successUrl);
        String role = (String) jwtUtils.parseToken(token).get("authorities");

        String url = getUrl(role);
        log.debug("Redirecting to: {}", url);
        ResponseCookie cookie = cookieUtils.createCookie("token", cookieProperties.getMaxAge(), token);
        ResponseCookie refreshIdCookie = cookieUtils.createCookie("refreshTokenId", cookieProperties.getMaxAge(), refreshTokenId);
        log.debug("Created cookies: {} {}", cookie, refreshIdCookie);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.setStatus(HttpServletResponse.SC_OK);
            response.addHeader("Set-Cookie", cookie.toString());
            response.addHeader("Set-Cookie", refreshIdCookie.toString());
            response.sendRedirect(url);
        } catch (Exception e) {
            log.error("Error during authentication: {}", e.getMessage());
        }

    }

    private String getUrl(String role) {

        if (Role.ROLE_TEMP.toString().equals(role)) {
            return urlProperties.getRoleSelectionUrl();
        } else if (Role.ROLE_BUYER.toString().equals(role)) {
            return urlProperties.getSuccessUrl();
        } else if (Role.ROLE_SELLER.toString().equals(role)) {
            return urlProperties.getSuccessUrl() + "/seller";
        }
        return urlProperties.getWithdrawnUrl() + "?error=role";
    }
}
