package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
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
        InquiryServiceAdminCrudTest.TestConfig.class,
        InquiryServiceImpl.class
})
@TestPropertySource(properties = {
        // H2 Database 설정
        "spring.datasource.url=jdbc:h2:mem:adminTestDb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@DisplayName("관리자 문의 서비스 테스트")
class InquiryServiceAdminCrudTest {

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

    private Users testUser;
    private final String PROVIDER_ID = "test-provider-1";
    private final String ADMIN_ID = "admin-1";

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .provider("google")
                .providerId(PROVIDER_ID)
                .userNameAttribute("name")
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .build();
        userRepository.save(testUser);
    }

    @Test
    @DisplayName("전체 문의 목록 조회 - 성공")
    void getAllInquiries_Success() {
        // given
        inquiryService.createInquiry(PROVIDER_ID, new InquiryCreateRequestDTO(
                InquiryType.PRODUCT, "문의1", "내용1", null, InquiryReceiveMethod.WEB
        ));
        inquiryService.createInquiry(PROVIDER_ID, new InquiryCreateRequestDTO(
                InquiryType.ORDER, "문의2", "내용2", null, InquiryReceiveMethod.WEB
        ));

        // when
        Page<InquiryListResponseDTO> result = inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).userName()).isEqualTo("테스트사용자");
        assertThat(result.getContent().get(0).inquiryNumber()).isEqualTo("#002"); // 최신순이므로 #002가 먼저
        assertThat(result.getContent().get(1).inquiryNumber()).isEqualTo("#001");
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("관리자 문의 상세 조회 - 성공")
    void getInquiryDetailForAdmin_Success() {
        // given
        InquiryResponseDTO created = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "관리자 조회 테스트", "관리자용 상세 내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryDetailResponseDTO result = inquiryService.getInquiryDetailForAdmin(created.inquiryId());

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("관리자 조회 테스트");
        assertThat(result.content()).isEqualTo("관리자용 상세 내용");
        assertThat(result.userName()).isEqualTo("테스트사용자");
        assertThat(result.inquiryStatus()).isEqualTo("답변 대기");
        assertThat(result.messages()).isEmpty();
    }

    @Test
    @DisplayName("관리자 답변 등록 - 성공 (상태 변화 확인)")
    void createAdminReply_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "답변 테스트", "문의 내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO replyRequest = InquiryRequestDTO.forContent("관리자 답변 내용입니다");

        // when
        InquiryResponseDTO result = inquiryService.createAdminReply(
                inquiry.inquiryId(), ADMIN_ID, replyRequest);

        // then
        assertThat(result.message()).isEqualTo("답변이 성공적으로 등록되었습니다.");

        // 상태 변화 확인: PENDING → ANSWERED
        InquiryDetailResponseDTO updated = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(updated.inquiryStatus()).isEqualTo("답변 완료");
        assertThat(updated.messages()).hasSize(1);
        assertThat(updated.messages().get(0).content()).isEqualTo("관리자 답변 내용입니다");
        assertThat(updated.messages().get(0).authorType()).isEqualTo("ADMIN");

        // DB 상태 확인
        Inquires rootInquiry = inquiryRepository.findById(inquiry.inquiryId()).orElse(null);
        assertThat(rootInquiry).isNotNull();
        assertThat(rootInquiry.getInquiryStatus()).isEqualTo(InquiryStatus.ANSWERED);
    }

    @Test
    @DisplayName("빈 내용으로 관리자 답변 등록 - 실패")
    void createAdminReply_EmptyContent() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO emptyRequest = InquiryRequestDTO.forContent("");

        // when & then
        assertThatThrownBy(() -> inquiryService.createAdminReply(
                inquiry.inquiryId(), ADMIN_ID, emptyRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("답변 내용은 필수입니다.");
    }

    @Test
    @DisplayName("관리자 강제 종료 - 성공")
    void closeInquiryByAdmin_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "강제 종료 테스트", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO closeRequest = InquiryRequestDTO.forClose("스팸 문의로 판단됨");

        // when
        InquiryResponseDTO result = inquiryService.closeInquiryByAdmin(
                inquiry.inquiryId(), ADMIN_ID, closeRequest);

        // then
        assertThat(result.status()).isEqualTo("강제 종료");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 종료되었습니다.");

        // DB 확인
        Inquires closedInquiry = inquiryRepository.findById(inquiry.inquiryId()).orElse(null);
        assertThat(closedInquiry).isNotNull();
        assertThat(closedInquiry.getInquiryStatus()).isEqualTo(InquiryStatus.FORCE_CLOSED);
    }

    @Test
    @DisplayName("사유 없이 관리자 강제 종료 - 실패")
    void closeInquiryByAdmin_NoReason() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO emptyReasonRequest = InquiryRequestDTO.forClose("");

        // when & then
        assertThatThrownBy(() -> inquiryService.closeInquiryByAdmin(
                inquiry.inquiryId(), ADMIN_ID, emptyReasonRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("강제 종료 시 사유는 필수입니다.");
    }

    @Test
    @DisplayName("긴급도 수정 - 성공")
    void updateUrgentLevel_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "긴급도 테스트", "내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryResponseDTO result = inquiryService.updateUrgentLevel(
                inquiry.inquiryId(), InquiryUrgentLevel.HIGH);

        // then
        assertThat(result.message()).isEqualTo("긴급도가 성공적으로 수정되었습니다.");

        // DB 확인
        Inquires updatedInquiry = inquiryRepository.findById(inquiry.inquiryId()).orElse(null);
        assertThat(updatedInquiry).isNotNull();
        assertThat(updatedInquiry.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);

        // 상세 조회로 확인
        InquiryDetailResponseDTO detail = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(detail.urgentLevel()).isEqualTo("높음");
    }

    @Test
    @DisplayName("관리자 팔로우업 답변 - 성공 (상태 변화 확인)")
    void createAdminFollowup_Success() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "팔로우업 테스트", "내용", null, InquiryReceiveMethod.WEB
                ));

        // 첫 번째 답변 (PENDING → ANSWERED)
        inquiryService.createAdminReply(inquiry.inquiryId(), ADMIN_ID,
                InquiryRequestDTO.forContent("첫 번째 답변"));

        // when - 두 번째 답변 (ANSWERED → FOLLOWUP)
        InquiryResponseDTO result = inquiryService.createAdminReply(
                inquiry.inquiryId(), ADMIN_ID, InquiryRequestDTO.forContent("추가 답변"));

        // then
        assertThat(result.message()).isEqualTo("답변이 성공적으로 등록되었습니다.");

        // 상태 변화 확인: ANSWERED → FOLLOWUP
        InquiryDetailResponseDTO detail = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(detail.messages()).hasSize(2);
        assertThat(detail.inquiryStatus()).isEqualTo("추가 문의"); // FOLLOWUP.getDisplayName() = "추가 문의"

        // DB 상태 확인
        Inquires rootInquiry = inquiryRepository.findById(inquiry.inquiryId()).orElse(null);
        assertThat(rootInquiry).isNotNull();
        assertThat(rootInquiry.getInquiryStatus()).isEqualTo(InquiryStatus.FOLLOWUP);
    }

    @Test
    @DisplayName("사용자 답글 후 관리자 재답변 - 복합 상태 변화")
    void userFollowupThenAdminReply_StatusChange() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "복합 상태 테스트", "내용", null, InquiryReceiveMethod.WEB
                ));

        // 관리자 첫 답변 (PENDING → ANSWERED)
        inquiryService.createAdminReply(inquiry.inquiryId(), ADMIN_ID,
                InquiryRequestDTO.forContent("관리자 답변"));

        // 사용자 팔로우업 (ANSWERED → FOLLOWUP)
        inquiryService.createUserFollowup(inquiry.inquiryId(), PROVIDER_ID,
                InquiryRequestDTO.forContent("사용자 추가 문의"));

        // when - 관리자 재답변 (FOLLOWUP → FOLLOWUP 유지)
        InquiryResponseDTO result = inquiryService.createAdminReply(
                inquiry.inquiryId(), ADMIN_ID, InquiryRequestDTO.forContent("관리자 재답변"));

        // then
        assertThat(result.message()).isEqualTo("답변이 성공적으로 등록되었습니다.");

        InquiryDetailResponseDTO detail = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(detail.messages()).hasSize(3); // 관리자답변 + 사용자팔로우업 + 관리자재답변
        assertThat(detail.inquiryStatus()).isEqualTo("추가 문의"); // FOLLOWUP.getDisplayName() = "추가 문의"

        // 메시지 순서 확인
        assertThat(detail.messages().get(0).authorType()).isEqualTo("ADMIN");
        assertThat(detail.messages().get(1).authorType()).isEqualTo("USER");
        assertThat(detail.messages().get(2).authorType()).isEqualTo("ADMIN");
    }
}