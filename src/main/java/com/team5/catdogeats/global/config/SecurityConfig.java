package com.team5.catdogeats.global.config;

import com.team5.catdogeats.auth.filter.JwtAuthenticationFilter;
import com.team5.catdogeats.auth.filter.PreventDuplicateLoginFilter;
import com.team5.catdogeats.auth.handler.CustomLogoutSuccessHandler;
import com.team5.catdogeats.auth.handler.OAuth2AuthenticationFailureHandler;
import com.team5.catdogeats.auth.handler.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/*

        - Redis HttpSession 임시 비활성화 + 세션 고정 보호 none() 적용 버전
- Toss Payments 콜백 URL(`/v1/buyers/payments/success|fail`)은 인증 없이 접근해야 하므로 permitAll() 추가.
  */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@Slf4j
@RequiredArgsConstructor
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> customOAuth2UserService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PreventDuplicateLoginFilter preventDuplicateLoginFilter;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;

    // ---------------------------------------------------------------------
    // 관리자 체인
    // ---------------------------------------------------------------------
    @Bean
    @Order(1)
    public SecurityFilterChain securityFilterChainAdmin(HttpSecurity http) {
        try {
            http.securityMatcher("/v1/admin/**")
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                            .sessionFixation().none()
                            .maximumSessions(1)
                            .maxSessionsPreventsLogin(false))
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/v1/admin/login", "/v1/admin/verify", "/v1/admin/resend-code").permitAll()
                            .requestMatchers("/v1/admin/invite", "/v1/admin/account-management", "/v1/admin/accounts/**").hasAuthority("ADMIN")
                            .requestMatchers("/v1/admin/**").authenticated())
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(l -> l.logoutUrl("/v1/admin/logout")
                            .logoutSuccessUrl("/v1/admin/login?logout=true")
                            .invalidateHttpSession(true)
                            .deleteCookies("JSESSIONID"))
                    .securityContext(ctx -> ctx.requireExplicitSave(false))
                    .exceptionHandling(eh -> eh.authenticationEntryPoint((req,res,e)->{
                        if(req.getRequestURI().startsWith("/v1/admin/")) res.sendRedirect("/v1/admin/login");
                    }).accessDeniedHandler((req,res,e)->{
                        if(req.getRequestURI().startsWith("/v1/admin/")) res.sendRedirect("/v1/admin/dashboard?error=access_denied");
                    }));
            return http.build();
        } catch (Exception e) {
            log.error("Admin SecurityFilterChain 오류", e);
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // 사용자 + Swagger + 결제 콜백 체인
    // ---------------------------------------------------------------------
    @Bean
    @Order(2)
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        try {
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                            .sessionFixation().none())
                    .authorizeHttpRequests(auth -> auth
                            // Swagger
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                            // 공개 엔드포인트
                            .requestMatchers("/", "/index.html", "WebSocket.html", "/withdraw", "/error", "/.well-known/**", "/ws/**").permitAll()
                            // OAuth2 redirect
                            .requestMatchers("/oauth2/**", "/login/oauth2/code/**").permitAll()
                            // 결제 콜백 (Toss)
                            .requestMatchers("/v1/buyers/payments/success", "/v1/buyers/payments/fail").permitAll()
                            // 공개 API
                            .requestMatchers("/v1/auth/refresh", "/v1/notices", "/v1/faqs").permitAll()
                            .requestMatchers("/v1/buyers/products/**", "/v1/buyers/reviews/**", "/v1/users/page/**").permitAll()
                            // 권한 API
                            .requestMatchers("/v1/users/**").hasAnyRole("BUYER", "SELLER")
                            .requestMatchers("/v1/sellers/**").hasRole("SELLER")
                            .requestMatchers("/v1/buyers/**").hasRole("BUYER")
                            .requestMatchers("/v1/auth/role").hasRole("TEMP")
                            .anyRequest().authenticated())
                    .oauth2Login(oauth2 -> oauth2
                            .successHandler(oAuth2AuthenticationSuccessHandler)
                            .failureHandler(oAuth2AuthenticationFailureHandler)
                            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService)))
                    .httpBasic(AbstractHttpConfigurer::disable)
                    .formLogin(AbstractHttpConfigurer::disable)
                    .logout(l -> l.logoutUrl("/v1/auth/logout")
                            .logoutSuccessHandler(customLogoutSuccessHandler)
                            .invalidateHttpSession(true)
                            .deleteCookies("token"))
                    .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                    .addFilterBefore(preventDuplicateLoginFilter, OAuth2AuthorizationRequestRedirectFilter.class);
            return http.build();
        } catch (Exception e) {
            log.error("User SecurityFilterChain 오류", e);
            throw new RuntimeException(e);
        }
    }

    // ---------------------------------------------------------------------
    // 지원 Bean
    // ---------------------------------------------------------------------
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

}
