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
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "cloud.aws.credentials.access-key=test-key",
        "cloud.aws.credentials.secret-key=test-secret",
        "cloud.aws.region.static=ap-northeast-2",
        "cloud.aws.s3.bucket=test-bucket",
        "cloud.aws.cloudfront.domain=test.cloudfront.net",
        "spring.batch.job.enabled=false"
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
    @DisplayName("전체 문의 목록 조회 - 긴급도 배치 처리 포함")
    void getAllInquiries() {
        // given
        inquiryService.createInquiry("test-provider-1", new InquiryCreateRequestDTO(
                InquiryType.PRODUCT, "문의1", "내용1", null, InquiryReceiveMethod.WEB
        ));

        // when
        Page<InquiryListResponseDTO> result = inquiryService.getAllInquiries(PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("문의1");
        assertThat(result.getContent().get(0).userName()).isEqualTo("테스트사용자");
        assertThat(result.getContent().get(0).inquiryNumber()).isEqualTo("#001");
    }

    @Test
    @DisplayName("관리자 문의 상세 조회")
    void getInquiryDetailForAdmin() {
        // given
        InquiryResponseDTO created = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의제목", "문의내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryDetailResponseDTO result = inquiryService.getInquiryDetailForAdmin(created.inquiryId());

        // then
        assertThat(result.inquiryId()).isEqualTo(created.inquiryId());
        assertThat(result.title()).isEqualTo("문의제목");
        assertThat(result.content()).isEqualTo("문의내용");
        assertThat(result.userName()).isEqualTo("테스트사용자");
        assertThat(result.messages()).isEmpty();
    }

    @Test
    @DisplayName("관리자 답변 등록")
    void createAdminReply() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO replyRequest = InquiryRequestDTO.forContent("답변 내용입니다");

        // when
        InquiryResponseDTO result = inquiryService.createAdminReply(
                inquiry.inquiryId(), "admin-1", replyRequest);

        // then
        assertThat(result.message()).isEqualTo("답변이 성공적으로 등록되었습니다.");

        // 원본 문의 상태 확인
        InquiryDetailResponseDTO updated = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(updated.inquiryStatus()).isEqualTo("답변 완료");
        assertThat(updated.messages()).hasSize(1);
        assertThat(updated.messages().get(0).authorType()).isEqualTo("ADMIN");
    }

    @Test
    @DisplayName("관리자 강제 종료")
    void closeInquiryByAdmin() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        InquiryRequestDTO closeRequest = InquiryRequestDTO.forClose("스팸 문의로 판단됨");

        // when
        InquiryResponseDTO result = inquiryService.closeInquiryByAdmin(
                inquiry.inquiryId(), "admin-1", closeRequest);

        // then
        assertThat(result.status()).isEqualTo("강제 종료");
        assertThat(result.message()).isEqualTo("문의가 성공적으로 종료되었습니다.");
    }

    @Test
    @DisplayName("긴급도 수정")
    void updateUrgentLevel() {
        // given
        InquiryResponseDTO inquiry = inquiryService.createInquiry("test-provider-1",
                new InquiryCreateRequestDTO(
                        InquiryType.PRODUCT, "문의", "내용", null, InquiryReceiveMethod.WEB
                ));

        // when
        InquiryResponseDTO result = inquiryService.updateUrgentLevel(
                inquiry.inquiryId(), InquiryUrgentLevel.HIGH);

        // then
        assertThat(result.message()).isEqualTo("긴급도가 성공적으로 수정되었습니다.");

        // 상세 조회로 확인
        InquiryDetailResponseDTO updated = inquiryService.getInquiryDetailForAdmin(inquiry.inquiryId());
        assertThat(updated.urgentLevel()).isEqualTo("높음");
    }
}