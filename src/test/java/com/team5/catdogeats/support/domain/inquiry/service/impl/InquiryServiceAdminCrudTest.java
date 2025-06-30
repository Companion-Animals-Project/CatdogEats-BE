package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
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
@Transactional
@DisplayName("관리자 문의 CRUD 테스트")
class InquiryServiceAdminCrudTest {

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
    @DisplayName("전체 문의 목록 조회")
    void getAllInquiries() {
        // given - 사용자가 문의 생성
        inquiryService.createInquiry("test-provider-1", new InquiryCreateRequestDTO(
                InquiryType.PRODUCT, "문의1", "내용1", null, InquiryReceiveMethod.WEB
        ));

        // when - 관리자가 전체 문의 조회
        Page<InquiryListResponseDTO> result = inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("문의1");
        assertThat(result.getContent().get(0).userName()).isEqualTo("테스트사용자");
    }

    @Test
    @DisplayName("관리자 문의 상세 조회")
    void getInquiryDetail() {
        // given - 사용자가 문의 생성
        InquiryResponseDTO created = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의제목", "문의내용", null, InquiryReceiveMethod.WEB
                ));

        // when - 관리자가 상세 조회
        InquiryDetailResponseDTO result = inquiryService.getInquiryDetailForAdmin(created.inquiryId());

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("문의제목");
        assertThat(result.content()).isEqualTo("문의내용");
        assertThat(result.userName()).isEqualTo("테스트사용자");
        assertThat(result.replies()).isEmpty();
    }

    @Test
    @DisplayName("관리자 답변 등록")
    void createReply() {
        // given - 사용자가 문의 생성
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryReplyRequestDTO replyRequest = new InquiryReplyRequestDTO(
                "답변 내용입니다",
                InquiryUrgentLevel.HIGH
        );

        // when - 관리자가 답변 등록
        InquiryResponseDTO result = inquiryService.createReply(inquiry.inquiryId(), "admin-1", replyRequest);

        // then
        assertThat(result.content()).isEqualTo("답변 내용입니다");
        assertThat(result.inquiryStatus()).isEqualTo("답변 완료");
        assertThat(result.urgentLevel()).isEqualTo("높음");

        // 원본 문의 상태 확인
        InquiryDetailResponseDTO updated = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(updated.inquiryStatus()).isEqualTo("답변 완료");
        assertThat(updated.replies()).hasSize(1);
    }
}