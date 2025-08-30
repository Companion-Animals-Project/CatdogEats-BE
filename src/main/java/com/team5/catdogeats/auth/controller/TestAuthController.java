package com.team5.catdogeats.auth.controller;

import com.team5.catdogeats.auth.dto.RotateTokenDTO;
import com.team5.catdogeats.auth.service.JwtService;
import com.team5.catdogeats.auth.service.RefreshTokenService;
import com.team5.catdogeats.auth.service.RotateRefreshTokenService;
import com.team5.catdogeats.auth.util.CookieUtils;
import com.team5.catdogeats.auth.util.JwtUtils;
import com.team5.catdogeats.global.config.CookieProperties;
import com.team5.catdogeats.global.dto.APIResponse;
import com.team5.catdogeats.global.enums.ResponseCode;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/test/auth")
@Tag(name = "Test Authentication", description = "성능 테스트용 인증 API")
@Profile("dev")
public class TestAuthController {

    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final RotateRefreshTokenService rotateRefreshTokenService;
    private final CookieUtils cookieUtils;
    private final CookieProperties cookieProperties;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    @Operation(summary = "테스트용 로그인", description = "성능 테스트를 위한 가짜 로그인 엔드포인트")
    public ResponseEntity<APIResponse<TestLoginResponse>> testLogin(
            @RequestBody TestLoginRequest request) {
        try {
            // 테스트용 사용자 조회 또는 생성
            Users testUser = getOrCreateTestUser(request.userId(), request.provider());

            // 가짜 Authentication 객체 생성
            Authentication authentication = createMockAuthentication(testUser);

            // JWT 토큰 생성
            String accessToken = jwtService.createAccessToken(authentication);
            String refreshTokenId = refreshTokenService.createRefreshToken(authentication);

            // 쿠키 생성
            ResponseCookie accessCookie = cookieUtils.createCookie("token",
                    cookieProperties.getMaxAge(), accessToken);
            ResponseCookie refreshCookie = cookieUtils.createCookie("refreshTokenId",
                    cookieProperties.getMaxAge(), refreshTokenId);

            TestLoginResponse response = new TestLoginResponse(
                    accessToken,
                    refreshTokenId,
                    testUser.getName(),
                    testUser.getRole()
            );

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                    .body(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("Test login failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @PostMapping("/refresh")
    @Operation(summary = "테스트용 토큰 갱신", description = "성능 테스트를 위한 토큰 갱신 엔드포인트")
    public ResponseEntity<APIResponse<RotateTokenDTO>> testRefresh(
            @RequestParam String refreshTokenId) {
        try {
            RotateTokenDTO dto = rotateRefreshTokenService.RotateRefreshToken(refreshTokenId);

            ResponseCookie accessCookie = cookieUtils.createCookie("token",
                    cookieProperties.getMaxAge(), dto.newAccessToken());
            ResponseCookie refreshIdCookie = cookieUtils.createCookie("refreshTokenId",
                    cookieProperties.getMaxAge(), dto.newRefreshToken());

            return ResponseEntity.ok()
                    .header(HttpHeaders.SET_COOKIE, accessCookie.toString())
                    .header(HttpHeaders.SET_COOKIE, refreshIdCookie.toString())
                    .body(APIResponse.success(ResponseCode.SUCCESS, dto));

        } catch (Exception e) {
            log.error("Test refresh failed", e);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(APIResponse.error(ResponseCode.ACCESS_DENIED));
        }
    }

    @PostMapping("/bulk-create-tokens")
    @Operation(summary = "대량 토큰 생성", description = "성능 테스트를 위한 대량 토큰 생성")
    public ResponseEntity<APIResponse<BulkTokenResponse>> createBulkTokens(
            @RequestParam(defaultValue = "100") int count,
            @RequestParam(defaultValue = "google") String provider) {
        try {
            var tokens = java.util.stream.IntStream.range(0, count)
                    .mapToObj(i -> {
                        String userId = "test-user-" + i + "-" + UUID.randomUUID().toString().substring(0, 8);
                        Users testUser = getOrCreateTestUser(userId, provider);
                        Authentication auth = createMockAuthentication(testUser);

                        String accessToken = jwtService.createAccessToken(auth);
                        String refreshTokenId = refreshTokenService.createRefreshToken(auth);

                        return new TokenPair(accessToken, refreshTokenId, testUser.getId());
                    })
                    .toList();

            BulkTokenResponse response = new BulkTokenResponse(tokens, count);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("Bulk token creation failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(APIResponse.error(ResponseCode.INTERNAL_SERVER_ERROR));
        }
    }

    @GetMapping("/validate")
    @Operation(summary = "토큰 검증", description = "토큰 유효성 검증")
    public ResponseEntity<APIResponse<TokenValidationResponse>> validateToken(
            @RequestParam String token) {
        try {
            boolean isValid = jwtUtils.validateToken(token);
            boolean isExpired = jwtUtils.isTokenExpired(token);

            TokenValidationResponse response = new TokenValidationResponse(isValid, isExpired);
            return ResponseEntity.ok(APIResponse.success(ResponseCode.SUCCESS, response));

        } catch (Exception e) {
            log.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(APIResponse.error(ResponseCode.INVALID_TOKEN));
        }
    }

    private Users getOrCreateTestUser(String userId, String provider) {
        return userRepository.findByProviderAndProviderId(provider, userId)
                .orElseGet(() -> {
                    Users newUser = Users.builder()
                            .provider(provider)
                            .providerId(userId)
                            .name("Test User " + userId)
                            .role(Role.ROLE_BUYER)
                            .userNameAttribute(getNameAttribute(provider))
                            .build();
                    return userRepository.save(newUser);
                });
    }

    private String getNameAttribute(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> "name";
            case "kakao" -> "profile_nickname";
            case "naver" -> "name";
            default -> "name";
        };
    }

    private Authentication createMockAuthentication(Users user) {
        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(user.getRole().toString());

        Map<String, Object> attributes = switch (user.getProvider().toLowerCase()) {
            case "google" -> Map.of(
                    "sub", user.getProviderId(),
                    "name", user.getName()
            );
            case "kakao" -> Map.of(
                    "id", user.getProviderId(),
                    "properties", Map.of("nickname", user.getName())
            );
            case "naver" -> Map.of(
                    "response", Map.of(
                            "id", user.getProviderId(),
                            "name", user.getName()
                    )
            );
            default -> Map.of("id", user.getProviderId(), "name", user.getName());
        };

        DefaultOAuth2User oAuth2User = new DefaultOAuth2User(
                Collections.singleton(authority),
                attributes,
                user.getUserNameAttribute()
        );

        return new OAuth2AuthenticationToken(
                oAuth2User,
                Collections.singleton(authority),
                user.getProvider()
        );
    }

    // DTO 클래스들
    public record TestLoginRequest(String userId, String provider) {}

    public record TestLoginResponse(String accessToken, String refreshTokenId,
                                    String name, Role role) {}

    public record TokenPair(String accessToken, String refreshTokenId, String userId) {}

    public record BulkTokenResponse(java.util.List<TokenPair> tokens, int count) {}

    public record TokenValidationResponse(boolean isValid, boolean isExpired) {}
}
