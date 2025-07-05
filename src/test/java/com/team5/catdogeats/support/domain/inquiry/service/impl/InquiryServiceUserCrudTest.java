package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryEscalationService;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
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

@DataJpaTest
@Import({
        InquiryServiceUserCrudTest.TestConfig.class,
        InquiryServiceImpl.class
})
@TestPropertySource(properties = {
        // H2 Database 설정
        "spring.datasource.url=jdbc:h2:mem:userTestDb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",

        // JPA 설정
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",

        // JWT 설정
        "jwt.secret=test-secret-key-for-testing-purposes-only-this-should-be-long-enough-for-jwt",
        "jwt.access-token-validity=3600",
        "jwt.refresh-token-validity=86400",

        // AWS 설정 (Mock)
        "cloud.aws.credentials.access-key=test-access-key",
        "cloud.aws.credentials.secret-key=test-secret-key",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.cloudfront.domain=test.cloudfront.net",

        // Admin 설정
        "app.admin.base-url=http://localhost:3000/admin",

        // Mail 설정 (Mock)
        "spring.mail.host=localhost",
        "spring.mail.port=587",
        "spring.mail.username=test",
        "spring.mail.password=test",

        // 기타 설정들
        "spring.batch.job.enabled=false",
        "logging.level.org.springframework.web=WARN",
        "logging.level.org.hibernate=WARN"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Transactional
@DisplayName("사용자 문의 서비스 테스트")
class InquiryServiceUserCrudTest {

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public JavaMailSender javaMailSender() {
            return mock(JavaMailSender.class);
        }

        @Bean
        @Primary
        public InquiryEscalationService inquiryEscalationService() {
            return mock(InquiryEscalationService.class);
        }

        @Bean("jpaTransactionManager")
        public org.springframework.transaction.PlatformTransactionManager jpaTransactionManager() {
            return new org.springframework.orm.jpa.JpaTransactionManager();
        }

        @Bean("transactionManager")
        @Primary
        public org.springframework.transaction.PlatformTransactionManager transactionManager() {
            return new org.springframework.orm.jpa.JpaTransactionManager();
        }
    }

    @Autowired
    private InquiryService inquiryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InquiryRepository inquiryRepository;

    private Users testUser1;
    private Users testUser2;
    private final String PROVIDER_ID_1 = "test-provider-1";
    private final String PROVIDER_ID_2 = "test-provider-2";

    @BeforeEach
    void setUp() {
        testUser1 = Users.builder()
                .provider("google")
                .providerId(PROVIDER_ID_1)
                .userNameAttribute("name")
                .name("테스트사용자1")
                .role(Role.ROLE_BUYER)
                .build();
        userRepository.save(testUser1);

        testUser2 = Users.builder()
                .provider("kakao")
                .providerId(PROVIDER_ID_2)
                .userNameAttribute("name")
                .name("테스트사용자2")
                .role(Role.ROLE_SELLER)
                .build();
        userRepository.save(testUser2);
    }

    @Test
    @DisplayName("사용자 문의 생성 - 성공")
    void createInquiry_Success() {
        // given
        InquiryCreateRequestDTO request = new InquiryCreateRequestDTO(
                InquiryType.PRODUCT,
                "제품 문의",
                "제품에 대한 상세한 문의 내용입니다.",
                null,
                InquiryReceiveMethod.WEB
        );

        // when
        InquiryResponseDTO result = inquiryService.createInquiry(PROVIDER_ID_1, request);

        // then
        assertThat(result.inquiryId()).isNotNull();
        assertThat(result.title()).isEqualTo("제품 문의");
        assertThat(result.status()).isEqualTo("답변 대기");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 등록되었습니다.");

        // DB 확인
        Inquires savedInquiry = inquiryRepository.findById(result.inquiryId()).orElse(null);
        assertThat(savedInquiry).isNotNull();
        assertThat(savedInquiry.getUsers().getId()).isEqualTo(testUser1.getId());
        assertThat(savedInquiry.getInquiryStatus()).isEqualTo(InquiryStatus.PENDING);
    }

    @Test
    @DisplayName("사용자별 문의 목록 조회 - 성공 (접근권한 검증 포함)")
    void getUserInquiries_Success() {
        // given
        // 사용자1의 문의 2개 생성
        inquiryService.createInquiry(PROVIDER_ID_1, new InquiryCreateRequestDTO(
                InquiryType.PRODUCT, "문의1", "내용1", null, InquiryReceiveMethod.WEB
        ));
        inquiryService.createInquiry(PROVIDER_ID_1, new InquiryCreateRequestDTO(
                InquiryType.ORDER, "문의2", "내용2", null, InquiryReceiveMethod.WEB
        ));

        // 사용자2의 문의 1개 생성 (섞이면 안됨)
        inquiryService.createInquiry(PROVIDER_ID_2, new InquiryCreateRequestDTO(
                InquiryType.DELIVERY, "다른사용자문의", "내용", null, InquiryReceiveMethod.WEB
        ));

        // when
        Page<InquiryListResponseDTO> result = inquiryService.getUserInquiries(
                PROVIDER_ID_1, PageRequest.of(0, 10));

        // then - 사용자1의 문의만 조회됨 (접근권한 검증)
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent()).allMatch(dto ->
                dto.title().equals("문의1") || dto.title().equals("문의2"));
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("사용자 문의 상세 조회 - 성공")
    void getUserInquiryDetail_Success() {
        // given
        InquiryResponseDTO created = inquiryService.createInquiry(PROVIDER_ID_1,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT,
                        "상세조회 테스트",
                        "상세 내용입니다",
                        null,
                        InquiryReceiveMethod.WEB
                ));

        // when
        InquiryDetailResponseDTO result = inquiryService.getUserInquiryDetail(
                created.inquiryId(), PROVIDER_ID_1);

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("상세조회 테스트");
        assertThat(result.content()).isEqualTo("상세 내용입니다");
        assertThat(result.inquiryStatus()).isEqualTo("답변 대기");
        assertThat(result.messages()).isEmpty();
    }

    @Test
    @DisplayName("사용자 답글 등록 - 성공")
    void createUserFollowup_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID_1,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO followupRequest = InquiryRequestDTO.forContent("추가 문의 내용입니다");

        // when
        InquiryResponseDTO result = inquiryService.createUserFollowup(
                inquiry.inquiryId(), PROVIDER_ID_1, followupRequest);

        // then
        assertThat(result.message()).isEqualTo("답글이 성공적으로 등록되었습니다.");

        // 원본 문의 상태 확인
        InquiryDetailResponseDTO updated = inquiryService.getUserInquiryDetail(
                inquiry.inquiryId(), PROVIDER_ID_1);
        assertThat(updated.messages()).hasSize(1);
        assertThat(updated.messages().get(0).content()).isEqualTo("추가 문의 내용입니다");
    }

    @Test
    @DisplayName("빈 내용으로 답글 등록 - 실패")
    void createUserFollowup_EmptyContent() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID_1,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO emptyRequest = InquiryRequestDTO.forContent("");

        // when & then
        assertThatThrownBy(() -> inquiryService.createUserFollowup(
                inquiry.inquiryId(), PROVIDER_ID_1, emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("내용은 필수입니다.");
    }

    @Test
    @DisplayName("사용자 문의 종료 - 성공")
    void closeInquiryByUser_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID_1,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryResponseDTO result = inquiryService.closeInquiryByUser(
                inquiry.inquiryId(), PROVIDER_ID_1);

        // then
        assertThat(result.status()).isEqualTo("문의 종료");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 종료되었습니다.");

        // DB 확인
        Inquires closedInquiry = inquiryRepository.findById(inquiry.inquiryId()).orElse(null);
        assertThat(closedInquiry).isNotNull();
        assertThat(closedInquiry.getInquiryStatus()).isEqualTo(InquiryStatus.CLOSED);
    }

    @Test
    @DisplayName("종료된 문의에 답글 등록 - 실패")
    void createUserFollowup_OnClosedInquiry() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID_1,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        inquiryService.closeInquiryByUser(inquiry.inquiryId(), PROVIDER_ID_1);

        InquiryRequestDTO followupRequest = InquiryRequestDTO.forContent("종료된 문의에 답글");

        // when & then
        assertThatThrownBy(() -> inquiryService.createUserFollowup(
                inquiry.inquiryId(), PROVIDER_ID_1, followupRequest))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("종료된 문의에는 답글을 등록할 수 없습니다.");
    }

    @Test
    @DisplayName("빈 문의 목록 조회 - 신규 사용자")
    void getUserInquiries_Empty() {
        // when - 문의를 생성하지 않은 새로운 사용자의 목록 조회
        Page<InquiryListResponseDTO> result = inquiryService.getUserInquiries(
                PROVIDER_ID_1, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}