package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@Import(InquiryServiceUserCrudTest.TestMailConfig.class)
@SpringBootTest
@TestPropertySource(properties = {
        // 기본 DB 설정
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",

        // 누락된 설정값들 추가 (이것이 핵심!)
        "jwt.secret=test-secret-key-for-testing-purposes-only",
        "jwt.access-token-validity=3600",
        "jwt.refresh-token-validity=86400",

        // AWS 설정
        "cloud.aws.credentials.access-key=test-key",
        "cloud.aws.credentials.secret-key=test-secret",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",

        // 메일 설정
        "spring.mail.host=localhost",
        "spring.mail.port=587",
        "spring.mail.username=test",
        "spring.mail.password=test",

        // 기타 설정들
        "spring.batch.job.enabled=false",
        "logging.level.org.springframework.web=DEBUG"
})
@Transactional
@DisplayName("사용자 문의 CRUD 테스트")
class InquiryServiceUserCrudTest {

    @TestConfiguration
    static class TestMailConfig {
        @Bean
        @Primary
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }
    }

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .provider("google")
                .providerId("test-provider-1")
                .userNameAttribute("name")
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("사용자 문의 생성")
    void createUserInquiry() {
        // given
        InquiryCreateRequestDTO request = new InquiryCreateRequestDTO(
                InquiryType.PRODUCT,
                "사용자 문의",
                "사용자 문의 내용",
                null,
                InquiryReceiveMethod.WEB
        );

        // when
        InquiryResponseDTO result = inquiryService.createInquiry("test-provider-1", request);

        // then
        assertThat(result.inquiryId()).isNotNull();
        assertThat(result.title()).isEqualTo("사용자 문의");
        assertThat(result.status()).isEqualTo("답변 대기");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 등록되었습니다.");
    }

    @Test
    @DisplayName("사용자별 문의 목록 조회")
    void getUserInquiries() {
        // given
        inquiryService.createInquiry("test-provider-1", new InquiryCreateRequestDTO(
                InquiryType.PRODUCT, "내 문의1", "내용1", null, InquiryReceiveMethod.WEB
        ));
        inquiryService.createInquiry("test-provider-1", new InquiryCreateRequestDTO(
                InquiryType.ORDER, "내 문의2", "내용2", null, InquiryReceiveMethod.WEB
        ));

        // when
        Page<InquiryListResponseDTO> result = inquiryService.getUserInquiries("test-provider-1", PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).title()).contains("내 문의");
        assertThat(result.getContent().get(1).title()).contains("내 문의");
    }

    @Test
    @DisplayName("사용자 문의 상세 조회")
    void getUserInquiryDetail() {
        // given
        InquiryResponseDTO created = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "상세조회 테스트", "상세내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryDetailResponseDTO result = inquiryService.getUserInquiryDetail(
                created.inquiryId(), "test-provider-1");

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("상세조회 테스트");
        assertThat(result.content()).isEqualTo("상세내용");
        assertThat(result.inquiryStatus()).isEqualTo("답변 대기");
        assertThat(result.messages()).isEmpty();
    }

    @Test
    @DisplayName("사용자 답글 등록")
    void createUserFollowup() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO followupRequest = InquiryRequestDTO.forContent("추가 문의 내용입니다");

        // when
        InquiryResponseDTO result = inquiryService.createUserFollowup(
                inquiry.inquiryId(), "test-provider-1", followupRequest);

        // then
        assertThat(result.message()).isEqualTo("답글이 성공적으로 등록되었습니다.");
    }

    @Test
    @DisplayName("사용자 문의 종료")
    void closeInquiryByUser() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryResponseDTO result = inquiryService.closeInquiryByUser(
                inquiry.inquiryId(), "test-provider-1");

        // then
        assertThat(result.status()).isEqualTo("문의 종료");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 종료되었습니다.");
    }

    @Test
    @DisplayName("종료된 문의에 답글 등록 시 예외 발생")
    void createFollowupOnClosedInquiry() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        inquiryService.closeInquiryByUser(inquiry.inquiryId(), "test-provider-1");

        InquiryRequestDTO followupRequest = InquiryRequestDTO.forContent("종료된 문의에 답글");

        // when & then
        assertThatThrownBy(() -> inquiryService.createUserFollowup(
                inquiry.inquiryId(), "test-provider-1", followupRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("종료된 문의에는 답글을 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("빈 문의 목록 조회")
    void getEmptyInquiries() {
        // given - 문의를 등록하지 않음

        // when
        Page<InquiryListResponseDTO> result = inquiryService.getUserInquiries("test-provider-1", PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}