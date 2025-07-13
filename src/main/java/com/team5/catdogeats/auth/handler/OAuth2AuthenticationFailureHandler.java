package com.team5.catdogeats.auth.handler;

import com.team5.catdogeats.auth.dto.UrlProperties;
import com.team5.catdogeats.global.exception.WithdrawnAccountException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
@RequiredArgsConstructor
public class OAuth2AuthenticationFailureHandler extends SimpleUrlAuthenticationFailureHandler {
    private final UrlProperties urlProperties;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception)
            throws IOException, ServletException {


        boolean withdrawn = exception instanceof WithdrawnAccountException
                || exception.getCause() instanceof WithdrawnAccountException;

        if (withdrawn) {
            String withdrawnUrl = urlProperties.getWithdrawnUrl();
            String url = withdrawnUrl + "?error=withdraw";
            getRedirectStrategy().sendRedirect(request, response, url);
            return;
        }

        String url = urlProperties.getLoginUrl();
        /* 3) 그 외 예외 */
        response.sendRedirect(url+"?error=oauth2");
    }

}
