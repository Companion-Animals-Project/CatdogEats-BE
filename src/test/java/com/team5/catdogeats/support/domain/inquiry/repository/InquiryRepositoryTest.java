package com.team5.catdogeats.support.domain.inquiry.repository;

import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryReceiveMethod;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
        "spring.jpa.properties.hibernate.format_sql=true"
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
        // 테스트용 사용자 생성
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
                .role(Role.ROLE_BUYER)
                .build();

        userRepository.save(testUser);
        userRepository.save(anotherUser);
    }

    @Test
    @DisplayName("사용자별 문의 최신순 조회")
    void findByUserIdOrderByCreatedAtDesc() throws InterruptedException {
        // given
        Inquires inquiry1 = createInquiry(testUser, "첫 번째 문의", "첫 번째 내용");
        inquiryRepository.save(inquiry1);

        // 시간 차이를 위한 대기
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

        // 다른 사용자의 문의는 포함되지 않음
        assertThat(result.getContent())
                .noneMatch(inquiry -> inquiry.getTitle().equals("다른 사용자 문의"));
    }

    @Test
    @DisplayName("전체 문의 최신순 조회")
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
    @DisplayName("최상위 문의만 조회")
    void findOnlyParentInquiries() {
        // given
        Inquires parentInquiry = createInquiry(testUser, "원본 문의", "원본 내용");
        inquiryRepository.save(parentInquiry);

        // 답변 (자식) 문의 생성
        Inquires replyInquiry = Inquires.builder()
                .parent(parentInquiry)
                .users(testUser)
                .title("Re: 원본 문의")
                .content("답변 내용")
                .inquiryType(InquiryType.PRODUCT)
                .inquiryReceiveMethod(InquiryReceiveMethod.WEB)
                .inquiryStatus(InquiryStatus.ANSWERED)
                .inquiryUrgentLevel(InquiryUrgentLevel.MEDIUM)
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
    @DisplayName("문의 답변 조회")
    void findByParent_Id() {
        // given
        Inquires parentInquiry = createInquiry(testUser, "원본 문의", "원본 내용");
        inquiryRepository.save(parentInquiry);

        // 답변 생성
        Inquires reply = createReply(parentInquiry, "관리자 답변 내용");
        inquiryRepository.save(reply);

        // when
        Optional<Inquires> result = inquiryRepository.findByParent_Id(parentInquiry.getId());

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getContent()).isEqualTo("관리자 답변 내용");
        assertThat(result.get().getParent().getId()).isEqualTo(parentInquiry.getId());
        assertThat(result.get().getInquiryStatus()).isEqualTo(InquiryStatus.ANSWERED);
    }

    @Test
    @DisplayName("답변 없는 문의 조회시 빈 결과")
    void findByParent_Id_NotFound() {
        // given
        Inquires parentInquiry = createInquiry(testUser, "답변 없는 문의", "답변 없는 내용");
        inquiryRepository.save(parentInquiry);

        // when
        Optional<Inquires> result = inquiryRepository.findByParent_Id(parentInquiry.getId());

        // then
        assertThat(result).isEmpty();
    }

    // 테스트용 문의 생성 헬퍼 메서드
    private Inquires createInquiry(Users user, String title, String content) {
        return Inquires.builder()
                .users(user)
                .title(title)
                .content(content)
                .inquiryType(InquiryType.PRODUCT)
                .inquiryReceiveMethod(InquiryReceiveMethod.WEB)
                .inquiryStatus(InquiryStatus.PENDING)
                .inquiryUrgentLevel(InquiryUrgentLevel.MEDIUM)
                .build();
    }

    // 테스트용 답변 생성 헬퍼 메서드
    private Inquires createReply(Inquires parent, String content) {
        return Inquires.builder()
                .parent(parent)
                .users(parent.getUsers())
                .title("Re: " + parent.getTitle())
                .content(content)
                .inquiryType(parent.getInquiryType())
                .inquiryReceiveMethod(parent.getInquiryReceiveMethod())
                .inquiryStatus(InquiryStatus.ANSWERED)
                .inquiryUrgentLevel(InquiryUrgentLevel.MEDIUM)
                .build();
    }
}