package com.team5.catdogeats.support.domain.reports.repository;

import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.repository.ReportRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import com.team5.catdogeats.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("ReportRepository 테스트")
class ReportRepositoryTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BuyerRepository buyerRepository;

    @Autowired
    private TestEntityManager entityManager;

    private Users testUser1;
    private Users testUser2;
    private Buyers testBuyer1;
    private Buyers testBuyer2;
    private Reports productReport1;
    private Reports productReport2;
    private Reports reviewReport1;
    private Reports reviewReport2;

    @BeforeEach
    void setUp() {
        // 테스트 사용자 생성
        testUser1 = Users.builder()
                .name("신고자1")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("reporter1")
                .provider("test")
                .providerId("test001")
                .build();

        testUser2 = Users.builder()
                .name("신고자2")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("reporter2")
                .provider("test")
                .providerId("test002")
                .build();

        // TestEntityManager로 저장
        entityManager.persistAndFlush(testUser1);
        entityManager.persistAndFlush(testUser2);

        // 테스트 구매자 생성
        testBuyer1 = Buyers.builder()
                .userId(testUser1.getId())
                .user(testUser1)
                .nameMaskingStatus(false)
                .isDeleted(false)
                .build();

        testBuyer2 = Buyers.builder()
                .userId(testUser2.getId())
                .user(testUser2)
                .nameMaskingStatus(false)
                .isDeleted(false)
                .build();

        // TestEntityManager로 저장
        entityManager.persistAndFlush(testBuyer1);
        entityManager.persistAndFlush(testBuyer2);

        // 테스트 신고 데이터 생성
        productReport1 = Reports.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer1)
                .reason("상품 정보 허위")
                .content("상품 설명과 실제 상품이 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .attachmentUrl("https://example.com/image1.jpg")
                .build();

        productReport2 = Reports.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer2)
                .reason("가격 허위")
                .content("가격이 다른 곳보다 너무 비쌉니다")
                .reportStatus(ReportStatus.COMPLETED)
                .processedByAdminId("admin123")
                .adminNote("확인 완료")
                .processedAt(ZonedDateTime.now().minusDays(1))
                .build();

        reviewReport1 = Reports.builder()
                .reportType(ReportType.REVIEW)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer1)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .reportStatus(ReportStatus.ON_HOLD)
                .build();

        reviewReport2 = Reports.builder()
                .reportType(ReportType.REVIEW)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer2)
                .reason("스팸/도배")
                .content("동일한 내용의 리뷰를 반복 작성합니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        // TestEntityManager로 저장
        entityManager.persistAndFlush(productReport1);
        entityManager.persistAndFlush(productReport2);
        entityManager.persistAndFlush(reviewReport1);
        entityManager.persistAndFlush(reviewReport2);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 모든 조건")
    void findReportsWithFilters_AllConditions() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(7);
        ZonedDateTime endDate = ZonedDateTime.now().plusDays(1);

        // when
        Page<Reports> result = reportRepository.findReportsWithFilters(
                ReportType.PRODUCT,
                ReportStatus.PENDING,
                "허위",
                startDate,
                endDate,
                pageable
        );

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getTotalElements()).isEqualTo(1);
        Reports foundReport = result.getContent().get(0);
        assertThat(foundReport.getReportType()).isEqualTo(ReportType.PRODUCT);
        assertThat(foundReport.getReportStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(foundReport.getReason()).contains("허위");
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 타입별 필터링")
    void findReportsWithFilters_ByReportType() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Reports> productReports = reportRepository.findReportsWithFilters(
                ReportType.PRODUCT, null, null, null, null, pageable);
        Page<Reports> reviewReports = reportRepository.findReportsWithFilters(
                ReportType.REVIEW, null, null, null, null, pageable);

        // then
        assertThat(productReports.getTotalElements()).isEqualTo(2);
        assertThat(reviewReports.getTotalElements()).isEqualTo(2);
        assertThat(productReports.getContent()).allMatch(r -> r.getReportType() == ReportType.PRODUCT);
        assertThat(reviewReports.getContent()).allMatch(r -> r.getReportType() == ReportType.REVIEW);
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 상태별 필터링")
    void findReportsWithFilters_ByStatus() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Reports> pendingReports = reportRepository.findReportsWithFilters(
                null, ReportStatus.PENDING, null, null, null, pageable);
        Page<Reports> completedReports = reportRepository.findReportsWithFilters(
                null, ReportStatus.COMPLETED, null, null, null, pageable);

        // then
        assertThat(pendingReports.getTotalElements()).isEqualTo(2);
        assertThat(completedReports.getTotalElements()).isEqualTo(1);
        assertThat(pendingReports.getContent()).allMatch(r -> r.getReportStatus() == ReportStatus.PENDING);
        assertThat(completedReports.getContent()).allMatch(r -> r.getReportStatus() == ReportStatus.COMPLETED);
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 키워드 검색")
    void findReportsWithFilters_ByKeyword() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when - reason 검색
        Page<Reports> reasonResults = reportRepository.findReportsWithFilters(
                null, null, "욕설", null, null, pageable);

        // when - content 검색
        Page<Reports> contentResults = reportRepository.findReportsWithFilters(
                null, null, "설명", null, null, pageable);

        // then
        assertThat(reasonResults.getTotalElements()).isEqualTo(1);
        assertThat(reasonResults.getContent().get(0).getReason()).contains("욕설");

        assertThat(contentResults.getTotalElements()).isEqualTo(1);
        assertThat(contentResults.getContent().get(0).getContent()).contains("설명");
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 날짜 범위 검색")
    void findReportsWithFilters_ByDateRange() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        ZonedDateTime yesterday = ZonedDateTime.now().minusDays(1);
        ZonedDateTime tomorrow = ZonedDateTime.now().plusDays(1);

        // when
        Page<Reports> result = reportRepository.findReportsWithFilters(
                null, null, null, yesterday, tomorrow, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4); // 모든 신고가 이 범위에 포함
        assertThat(result.getContent()).allMatch(r ->
                r.getCreatedAt().isAfter(yesterday) && r.getCreatedAt().isBefore(tomorrow));
    }

    @Test
    @DisplayName("복잡한 검색 조건으로 신고 목록 조회 - 전체 조회 (필터 없음)")
    void findReportsWithFilters_NoFilters() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Reports> result = reportRepository.findReportsWithFilters(
                null, null, null, null, null, pageable);

        // then
        assertThat(result.getTotalElements()).isEqualTo(4);
        assertThat(result.getContent()).hasSize(4);
        // 최신순 정렬 확인
        List<Reports> reports = result.getContent();
        for (int i = 0; i < reports.size() - 1; i++) {
            assertThat(reports.get(i).getCreatedAt())
                    .isAfterOrEqualTo(reports.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("특정 사용자의 신고 목록 조회")
    void findByReporterIdOrderByCreatedAtDesc() {
        // given
        Pageable pageable = PageRequest.of(0, 10);

        // when
        Page<Reports> user1Reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(
                testUser1.getId(), pageable);
        Page<Reports> user2Reports = reportRepository.findByReporterIdOrderByCreatedAtDesc(
                testUser2.getId(), pageable);

        // then
        assertThat(user1Reports.getTotalElements()).isEqualTo(2);
        assertThat(user2Reports.getTotalElements()).isEqualTo(2);

        // 신고자 확인
        assertThat(user1Reports.getContent()).allMatch(r ->
                r.getReporter().getUserId().equals(testUser1.getId()));
        assertThat(user2Reports.getContent()).allMatch(r ->
                r.getReporter().getUserId().equals(testUser2.getId()));

        // 최신순 정렬 확인
        List<Reports> user1ReportsList = user1Reports.getContent();
        for (int i = 0; i < user1ReportsList.size() - 1; i++) {
            assertThat(user1ReportsList.get(i).getCreatedAt())
                    .isAfterOrEqualTo(user1ReportsList.get(i + 1).getCreatedAt());
        }
    }

    @Test
    @DisplayName("특정 대상에 대한 신고 목록 조회")
    void findByTargetIdOrderByCreatedAtDesc() {
        // given
        String targetId = productReport1.getTargetId();

        // when
        List<Reports> result = reportRepository.findByTargetIdOrderByCreatedAtDesc(targetId);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTargetId()).isEqualTo(targetId);
        assertThat(result.get(0).getReportType()).isEqualTo(ReportType.PRODUCT);
    }

    @Test
    @DisplayName("중복 신고 체크 - 기존 신고가 있는 경우")
    void findExistingReport_Exists() {
        // given
        String reporterId = testBuyer1.getUserId();
        String targetId = productReport1.getTargetId();

        // when
        Optional<Reports> result = reportRepository.findExistingReport(reporterId, targetId);

        // then
        assertThat(result).isPresent();
        assertThat(result.get().getReporter().getUserId()).isEqualTo(reporterId);
        assertThat(result.get().getTargetId()).isEqualTo(targetId);
    }

    @Test
    @DisplayName("중복 신고 체크 - 기존 신고가 없는 경우")
    void findExistingReport_NotExists() {
        // given
        String reporterId = testBuyer1.getUserId();
        String nonExistentTargetId = UUID.randomUUID().toString();

        // when
        Optional<Reports> result = reportRepository.findExistingReport(reporterId, nonExistentTargetId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("신고 상세 조회 - 연관 엔티티 포함")
    void findByIdWithDetails() {
        // given
        String reportId = productReport1.getId();

        // when
        Optional<Reports> result = reportRepository.findByIdWithDetails(reportId);

        // then
        assertThat(result).isPresent();
        Reports report = result.get();
        assertThat(report.getId()).isEqualTo(reportId);

        // 연관 엔티티 Fetch 확인
        assertThat(report.getReporter()).isNotNull();
        assertThat(report.getReporter().getUser()).isNotNull();
        assertThat(report.getReporter().getUser().getName()).isEqualTo("신고자1");
    }

    @Test
    @DisplayName("신고 상세 조회 - 존재하지 않는 신고")
    void findByIdWithDetails_NotFound() {
        // given
        String nonExistentReportId = UUID.randomUUID().toString();

        // when
        Optional<Reports> result = reportRepository.findByIdWithDetails(nonExistentReportId);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("페이징 테스트 - 페이지 크기와 번호")
    void findReportsWithFilters_Paging() {
        // given
        Pageable firstPage = PageRequest.of(0, 2);
        Pageable secondPage = PageRequest.of(1, 2);

        // when
        Page<Reports> firstPageResult = reportRepository.findReportsWithFilters(
                null, null, null, null, null, firstPage);
        Page<Reports> secondPageResult = reportRepository.findReportsWithFilters(
                null, null, null, null, null, secondPage);

        // then
        assertThat(firstPageResult.getContent()).hasSize(2);
        assertThat(secondPageResult.getContent()).hasSize(2);
        assertThat(firstPageResult.getTotalElements()).isEqualTo(4);
        assertThat(firstPageResult.getTotalPages()).isEqualTo(2);
        assertThat(firstPageResult.hasNext()).isTrue();
        assertThat(secondPageResult.hasNext()).isFalse();

        // 페이지별 다른 데이터 확인
        List<String> firstPageIds = firstPageResult.getContent().stream()
                .map(Reports::getId)
                .toList();
        List<String> secondPageIds = secondPageResult.getContent().stream()
                .map(Reports::getId)
                .toList();

        assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds);
    }

    @Test
    @DisplayName("비즈니스 메서드 테스트 - updateStatus")
    void businessMethod_UpdateStatus() {
        // given
        Reports report = productReport1;
        String adminId = "admin456";
        String adminNote = "검토 완료";

        // when
        report.updateStatus(ReportStatus.COMPLETED, adminId, adminNote);
        reportRepository.save(report);
        entityManager.flush();

        // then
        Reports updatedReport = reportRepository.findById(report.getId()).orElseThrow();
        assertThat(updatedReport.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(updatedReport.getProcessedByAdminId()).isEqualTo(adminId);
        assertThat(updatedReport.getAdminNote()).isEqualTo(adminNote);
        assertThat(updatedReport.getProcessedAt()).isNotNull();
        assertThat(updatedReport.isProcessed()).isTrue();
    }

    @Test
    @DisplayName("비즈니스 메서드 테스트 - 신고 타입 확인")
    void businessMethod_ReportTypeCheck() {
        // when & then
        assertThat(productReport1.isProductReport()).isTrue();
        assertThat(productReport1.isReviewReport()).isFalse();

        assertThat(reviewReport1.isProductReport()).isFalse();
        assertThat(reviewReport1.isReviewReport()).isTrue();
    }

    @Test
    @DisplayName("비즈니스 메서드 테스트 - 처리 상태 확인")
    void businessMethod_ProcessedStatusCheck() {
        // when & then
        assertThat(productReport1.isProcessed()).isFalse(); // PENDING
        assertThat(productReport2.isProcessed()).isTrue();  // COMPLETED
        assertThat(reviewReport1.isProcessed()).isTrue();   // ON_HOLD
    }
}