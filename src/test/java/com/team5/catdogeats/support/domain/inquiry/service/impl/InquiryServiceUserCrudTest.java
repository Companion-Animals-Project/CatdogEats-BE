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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        // 데이터베이스 설정 (H2 In-Memory)
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",

        // AWS 설정 (테스트용 더미값 - Bean 생성 오류 방지용)
        "cloud.aws.credentials.access-key=test-key",
        "cloud.aws.credentials.secret-key=test-secret",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.cloudfront.domain=test.cloudfront.net",

        // 기타 설정 비활성화
        "spring.batch.job.enabled=false",
        "spring.rabbitmq.host=localhost",
        "spring.data.redis.host=localhost"
})
@Transactional  // 일반 @Transactional 사용 (jpaTransactionManager 대신)
@DisplayName("사용자 문의 CRUD 테스트")
class InquiryServiceUserCrudTest {

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
        assertThat(result.content()).isEqualTo("사용자 문의 내용");
        assertThat(result.userName()).isEqualTo("테스트사용자");
        assertThat(result.inquiryStatus()).isEqualTo("답변 대기");
        assertThat(result.inquiryType()).isEqualTo("제품");
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
        Page<UserInquiryListResponseDTO> result = inquiryService.getUserInquiries("test-provider-1", PageRequest.of(0, 10));

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
        UserInquiryDetailResponseDTO result = inquiryService.getUserInquiryDetail(
                created.inquiryId(), "test-provider-1");

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("상세조회 테스트");
        assertThat(result.content()).isEqualTo("상세내용");
        assertThat(result.inquiryStatus()).isEqualTo("답변 대기");
        assertThat(result.replies()).isEmpty();
    }

    @Test
    @DisplayName("빈 문의 목록 조회")
    void getEmptyInquiries() {
        // given - 문의를 등록하지 않음

        // when
        Page<UserInquiryListResponseDTO> result = inquiryService.getUserInquiries("test-provider-1", PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isEqualTo(0);
    }
}