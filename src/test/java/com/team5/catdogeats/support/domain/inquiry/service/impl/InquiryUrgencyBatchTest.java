package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryCreateRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryResponseDTO;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZonedDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@DataJpaTest
@Import({
        InquiryUrgencyBatchTest.TestConfig.class,
        InquiryServiceImpl.class
})
@TestPropertySource(properties = {
        // H2 Database 설정
        "spring.datasource.url=jdbc:h2:mem:urgencyBatchTestDb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
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
@DisplayName("문의 긴급도 배치 처리 테스트")
class InquiryUrgencyBatchTest {

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

    @Autowired
    private TestEntityManager entityManager;

    private Users testUser;
    private final String PROVIDER_ID = "test-provider-1";

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
    @DisplayName("제품 문의 배치 처리 - 48시간 경과 시 MIDDLE → HIGH 상승")
    void productInquiryBatch_EscalateAfter48Hours() {
        // given - 49시간 전 제품 문의 생성
        InquiryResponseDTO recentInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.PRODUCT, "최근 제품 문의", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO oldInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.PRODUCT, "오래된 제품 문의", "내용", null, InquiryReceiveMethod.WEB));

        // 특정 문의의 생성시간을 49시간 전으로 변경
        Inquires oldInquiryEntity = inquiryRepository.findById(oldInquiry.inquiryId()).get();
        ZonedDateTime pastTime = ZonedDateTime.now().minusHours(49);
        setCreatedAt(oldInquiryEntity, pastTime);

        // 초기 상태 확인
        assertThat(oldInquiryEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);

        Inquires recentInquiryEntity = inquiryRepository.findById(recentInquiry.inquiryId()).get();
        assertThat(recentInquiryEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);

        // when - getAllInquiries 호출 시 배치 처리 발생
        Page<InquiryListResponseDTO> result = inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then - 오래된 문의는 HIGH로 상승, 최근 문의는 유지
        entityManager.flush();
        entityManager.clear();

        Inquires updatedOldInquiry = inquiryRepository.findById(oldInquiry.inquiryId()).get();
        Inquires updatedRecentInquiry = inquiryRepository.findById(recentInquiry.inquiryId()).get();

        assertThat(updatedOldInquiry.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(updatedRecentInquiry.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);

        // 결과에도 반영되는지 확인
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    @DisplayName("주문/배송/계정 문의 배치 처리 - 72시간 경과 시 MIDDLE → HIGH 상승")
    void standardInquiryBatch_EscalateAfter72Hours() {
        // given - 73시간 전 주문 문의와 71시간 전 배송 문의 생성
        InquiryResponseDTO orderInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ORDER, "주문 문의", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO deliveryInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.DELIVERY, "배송 문의", "내용", null, InquiryReceiveMethod.WEB));

        // 시간 조작
        Inquires orderEntity = inquiryRepository.findById(orderInquiry.inquiryId()).get();
        Inquires deliveryEntity = inquiryRepository.findById(deliveryInquiry.inquiryId()).get();

        setCreatedAt(orderEntity, ZonedDateTime.now().minusHours(73)); // 상승 예상
        setCreatedAt(deliveryEntity, ZonedDateTime.now().minusHours(71)); // 상승 안됨

        // when - 배치 처리 실행
        inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then
        entityManager.flush();
        entityManager.clear();

        Inquires updatedOrderEntity = inquiryRepository.findById(orderInquiry.inquiryId()).get();
        Inquires updatedDeliveryEntity = inquiryRepository.findById(deliveryInquiry.inquiryId()).get();

        assertThat(updatedOrderEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(updatedDeliveryEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);
    }

    @Test
    @DisplayName("기타 문의 배치 처리 - 72시간 경과 시 LOW → MIDDLE, 120시간 경과 시 MIDDLE → HIGH")
    void etcInquiryBatch_TwoStepEscalation() {
        // given - 기타 문의 3개 생성 (각각 다른 시간대)
        InquiryResponseDTO etcInquiry1 = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ETC, "기타 문의1", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO etcInquiry2 = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ETC, "기타 문의2", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO etcInquiry3 = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ETC, "기타 문의3", "내용", null, InquiryReceiveMethod.WEB));

        // 시간 조작
        Inquires entity1 = inquiryRepository.findById(etcInquiry1.inquiryId()).get();
        Inquires entity2 = inquiryRepository.findById(etcInquiry2.inquiryId()).get();
        Inquires entity3 = inquiryRepository.findById(etcInquiry3.inquiryId()).get();

        setCreatedAt(entity1, ZonedDateTime.now().minusHours(50)); // 상승 없음 (LOW 유지)
        setCreatedAt(entity2, ZonedDateTime.now().minusHours(75)); // LOW → MIDDLE
        setCreatedAt(entity3, ZonedDateTime.now().minusHours(125)); // LOW → HIGH (최종 단계)

        // 초기 상태 확인 (모두 LOW)
        assertThat(entity1.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.LOW);
        assertThat(entity2.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.LOW);
        assertThat(entity3.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.LOW);

        // when - 배치 처리 실행
        inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then
        entityManager.flush();
        entityManager.clear();

        Inquires updated1 = inquiryRepository.findById(etcInquiry1.inquiryId()).get();
        Inquires updated2 = inquiryRepository.findById(etcInquiry2.inquiryId()).get();
        Inquires updated3 = inquiryRepository.findById(etcInquiry3.inquiryId()).get();

        assertThat(updated1.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.LOW); // 변화 없음
        assertThat(updated2.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE); // 1단계 상승
        assertThat(updated3.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH); // 최종 단계 상승
    }

    @Test
    @DisplayName("결제/환불 문의 배치 처리 - HIGH 상태로 이미 높아서 상승하지 않음")
    void paymentInquiryBatch_NoEscalationForHighPriority() {
        // given - 결제/환불 문의 생성 (기본적으로 HIGH)
        InquiryResponseDTO paymentInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.PAYMENT, "결제 문의", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO returnInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.RETURN, "환불 문의", "내용", null, InquiryReceiveMethod.WEB));

        // 시간을 충분히 과거로 설정
        Inquires paymentEntity = inquiryRepository.findById(paymentInquiry.inquiryId()).get();
        Inquires returnEntity = inquiryRepository.findById(returnInquiry.inquiryId()).get();

        setCreatedAt(paymentEntity, ZonedDateTime.now().minusHours(100));
        setCreatedAt(returnEntity, ZonedDateTime.now().minusHours(100));

        // 초기 상태 확인 (이미 HIGH)
        assertThat(paymentEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(returnEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);

        // when - 배치 처리 실행
        inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then - 변화 없음 (이미 최고 단계)
        entityManager.flush();
        entityManager.clear();

        Inquires updatedPayment = inquiryRepository.findById(paymentInquiry.inquiryId()).get();
        Inquires updatedReturn = inquiryRepository.findById(returnInquiry.inquiryId()).get();

        assertThat(updatedPayment.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(updatedReturn.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
    }

    @Test
    @DisplayName("답변 완료된 문의는 배치 처리에서 제외")
    void answeredInquiryBatch_NoEscalationForAnswered() {
        // given - 오래된 문의를 생성하고 답변 완료 상태로 변경
        InquiryResponseDTO inquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.PRODUCT, "답변 완료된 문의", "내용", null, InquiryReceiveMethod.WEB));

        Inquires inquiryEntity = inquiryRepository.findById(inquiry.inquiryId()).get();
        setCreatedAt(inquiryEntity, ZonedDateTime.now().minusHours(100)); // 충분히 오래된 시간
        inquiryEntity.setInquiryStatus(InquiryStatus.ANSWERED); // 답변 완료로 설정

        // 초기 상태 확인
        assertThat(inquiryEntity.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);
        assertThat(inquiryEntity.getInquiryStatus()).isEqualTo(InquiryStatus.ANSWERED);

        // when - 배치 처리 실행
        inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then - 답변 완료된 문의는 긴급도 상승하지 않음
        entityManager.flush();
        entityManager.clear();

        Inquires updatedInquiry = inquiryRepository.findById(inquiry.inquiryId()).get();
        assertThat(updatedInquiry.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE); // 변화 없음
    }

    @Test
    @DisplayName("혼합 시나리오 배치 처리 - 다양한 문의 유형과 시간대")
    void mixedScenarioBatch_VariousTypesAndTimeframes() {
        // given - 다양한 유형의 문의들 생성
        InquiryResponseDTO productInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.PRODUCT, "제품 문의", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO orderInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ORDER, "주문 문의", "내용", null, InquiryReceiveMethod.WEB));

        InquiryResponseDTO etcInquiry = inquiryService.createInquiry(PROVIDER_ID,
                new InquiryCreateRequestDTO(InquiryType.ETC, "기타 문의", "내용", null, InquiryReceiveMethod.WEB));

        // 시간 조작 - 모두 상승 조건 만족
        setCreatedAt(inquiryRepository.findById(productInquiry.inquiryId()).get(),
                ZonedDateTime.now().minusHours(50)); // MIDDLE → HIGH
        setCreatedAt(inquiryRepository.findById(orderInquiry.inquiryId()).get(),
                ZonedDateTime.now().minusHours(75)); // MIDDLE → HIGH
        setCreatedAt(inquiryRepository.findById(etcInquiry.inquiryId()).get(),
                ZonedDateTime.now().minusHours(75)); // LOW → MIDDLE

        // when - 배치 처리 실행
        Page<InquiryListResponseDTO> result = inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then - 각각 예상된 대로 상승
        entityManager.flush();
        entityManager.clear();

        Inquires updatedProduct = inquiryRepository.findById(productInquiry.inquiryId()).get();
        Inquires updatedOrder = inquiryRepository.findById(orderInquiry.inquiryId()).get();
        Inquires updatedEtc = inquiryRepository.findById(etcInquiry.inquiryId()).get();

        assertThat(updatedProduct.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(updatedOrder.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.HIGH);
        assertThat(updatedEtc.getInquiryUrgentLevel()).isEqualTo(InquiryUrgentLevel.MIDDLE);

        // 조회 결과 검증
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getTotalElements()).isEqualTo(3);
    }

    // Helper method - 테스트용 시간 설정
    private void setCreatedAt(Inquires inquiry, ZonedDateTime createdAt) {
        // JPA의 @CreatedDate 필드를 테스트에서 직접 수정하기 위한 리플렉션 사용
        try {
            java.lang.reflect.Field createdAtField = inquiry.getClass().getSuperclass().getDeclaredField("createdAt");
            createdAtField.setAccessible(true);
            createdAtField.set(inquiry, createdAt);
            entityManager.merge(inquiry);
            entityManager.flush();
        } catch (Exception e) {
            throw new RuntimeException("Failed to set createdAt for test", e);
        }
    }
}