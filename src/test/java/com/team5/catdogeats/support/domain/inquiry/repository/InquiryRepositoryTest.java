package com.team5.catdogeats.support.domain.inquiry.repository;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.*;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false"
})
@DisplayName("문의 저장소 테스트")
class InquiryRepositoryTest {

    @Autowired
    private InquiryRepository inquiryRepository;

    @Autowired
    private UserRepository userRepository;

    private Users testUser;
    private Users anotherUser;

    @BeforeEach
    void setUp() {
        testUser = Users.builder()
                .provider("google")
                .providerId("test-provider-id-1")
                .userNameAttribute("name")
                .name("테스트 사용자1")
                .role(Role.ROLE_BUYER)
                .build();

        anotherUser = Users.builder()
                .provider("google")
                .providerId("test-provider-id-2")
                .userNameAttribute("name")
                .name("테스트 사용자2")
                .role(Role.ROLE_SELLER)
                .build();

        userRepository.save(testUser);
        userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("사용자별 문의 최신순 조회 - 성능 최적화 쿼리")
    void findByUserIdOrderByCreatedAtDesc() throws InterruptedException {
        // given
        Inquires inquiry1 = createInquiry(testUser, "첫 번째 문의", "첫 번째 내용");
        inquiryRepository.save(inquiry1);

        Thread.sleep(10);

        Inquires inquiry2 = createInquiry(testUser, "두 번째 문의", "두 번째 내용");
        inquiryRepository.save(inquiry2);

        Thread.sleep(10);

        Inquires inquiry3 = createInquiry(anotherUser, "다른 사용자 문의", "다른 사용자 내용");
        inquiryRepository.save(inquiry3);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Inquires> result = inquiryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("두 번째 문의");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("첫 번째 문의");
        assertThat(result.getContent())
                .noneMatch(inquiry -> inquiry.getTitle().equals("다른 사용자 문의"));
    }

    @Test
    @DisplayName("전체 문의 최신순 조회 - 관리자용")
    void findAllInquiriesOrderByCreatedAtDesc() throws InterruptedException {
        // given
        Inquires inquiry1 = createInquiry(testUser, "문의1", "내용1");
        inquiryRepository.save(inquiry1);

        Thread.sleep(10);

        Inquires inquiry2 = createInquiry(anotherUser, "문의2", "내용2");
        inquiryRepository.save(inquiry2);

        Thread.sleep(10);

        Inquires inquiry3 = createInquiry(testUser, "문의3", "내용3");
        inquiryRepository.save(inquiry3);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Inquires> result = inquiryRepository.findAllInquiriesOrderByCreatedAtDesc(pageable);

        // then
        assertThat(result.getContent()).hasSize(3);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("문의3");
        assertThat(result.getContent().get(1).getTitle()).isEqualTo("문의2");
        assertThat(result.getContent().get(2).getTitle()).isEqualTo("문의1");
    }

    @Test
    @DisplayName("최상위 문의만 조회 - 답글 제외")
    void findOnlyParentInquiries() {
        // given
        Inquires parentInquiry = createInquiry(testUser, "원본 문의", "원본 내용");
        inquiryRepository.save(parentInquiry);

        // 답글 문의 생성
        Inquires replyInquiry = Inquires.builder()
                .parent(parentInquiry)
                .users(testUser)
                .title("Re: 원본 문의")
                .content("답변 내용")
                .inquiryType(InquiryType.PRODUCT)
                .inquiryReceiveMethod(InquiryReceiveMethod.WEB)
                .inquiryStatus(InquiryStatus.ANSWERED)
                .inquiryUrgentLevel(InquiryUrgentLevel.MIDDLE)
                .inquiryMessageType(InquiryMessageType.ADMIN_FOLLOWUP)
                .build();
        inquiryRepository.save(replyInquiry);

        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Inquires> result = inquiryRepository.findByUserIdOrderByCreatedAtDesc(testUser.getId(), pageable);

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getTitle()).isEqualTo("원본 문의");
        assertThat(result.getContent().get(0).getParent()).isNull();
    }

    @Test
    @DisplayName("문의 답글들 시간순 조회")
    void findByParentIdOrderByCreatedAtAsc() throws InterruptedException {
        // given
        Inquires parentInquiry = createInquiry(testUser, "원본 문의", "원본 내용");
        inquiryRepository.save(parentInquiry);

        Thread.sleep(10);

        // 관리자 답변
        Inquires adminReply = createReply(parentInquiry, "관리자 답변", InquiryMessageType.ANSWER);
        inquiryRepository.save(adminReply);

        Thread.sleep(10);

        // 사용자 추가 문의
        Inquires userFollowup = createReply(parentInquiry, "사용자 추가 문의", InquiryMessageType.USER_FOLLOWUP);
        inquiryRepository.save(userFollowup);

        // when
        List<Inquires> result = inquiryRepository.findByParentIdOrderByCreatedAtAsc(parentInquiry.getId());

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).getContent()).isEqualTo("관리자 답변");
        assertThat(result.get(1).getContent()).isEqualTo("사용자 추가 문의");
    }


    // 헬퍼 메서드들
    private Inquires createInquiry(Users user, String title, String content) {
        return Inquires.builder()
                .users(user)
                .title(title)
                .content(content)
                .inquiryType(InquiryType.PRODUCT)
                .inquiryReceiveMethod(InquiryReceiveMethod.WEB)
                .inquiryStatus(InquiryStatus.PENDING)
                .inquiryUrgentLevel(InquiryUrgentLevel.MIDDLE)
                .inquiryMessageType(InquiryMessageType.QUESTION)
                .build();
    }

    private Inquires createReply(Inquires parent, String content, InquiryMessageType messageType) {
        return Inquires.builder()
                .parent(parent)
                .users(parent.getUsers())
                .title("Re: " + parent.getTitle())
                .content(content)
                .inquiryType(parent.getInquiryType())
                .inquiryReceiveMethod(parent.getInquiryReceiveMethod())
                .inquiryStatus(InquiryStatus.ANSWERED)
                .inquiryUrgentLevel(InquiryUrgentLevel.MIDDLE)
                .inquiryMessageType(messageType)
                .build();
    }
}