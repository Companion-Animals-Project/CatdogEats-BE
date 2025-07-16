package com.team5.catdogeats.auth.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class SseSilentAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        if (response.isCommitted()) {
            log.error("SSE Access Denied: 응답 이미 전송됨");
            return;
        }

        try {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "권한이 없습니다.");
        } catch (IOException e) {

        }
    }
}
