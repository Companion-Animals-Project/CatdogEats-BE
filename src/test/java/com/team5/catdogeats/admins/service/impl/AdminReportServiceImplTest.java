package com.team5.catdogeats.admins.service.impl;

import com.team5.catdogeats.admins.domain.dto.ReportDetailResponseDto;
import com.team5.catdogeats.admins.domain.dto.ReportSearchDto;
import com.team5.catdogeats.admins.domain.dto.ReportStatusUpdateDto;
import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.exception.ReportNotFoundException;
import com.team5.catdogeats.support.repository.ReportRepository;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminReportService 테스트")
class AdminReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private AdminReportServiceImpl adminReportService;

    private Buyers testBuyer;
    private Products testProduct;
    private Reviews testReview;
    private Reports testReport;
    private String reportId;
    private String productId;
    private String reviewId;
    private String adminId;

    @BeforeEach
    void setUp() {
        reportId = UUID.randomUUID().toString();
        productId = UUID.randomUUID().toString();
        reviewId = UUID.randomUUID().toString();
        adminId = "admin123";

        // 테스트 사용자 생성
        Users testUser = Users.builder()
                .id(UUID.randomUUID().toString())
                .name("신고자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("reporter")
                .provider("test")
                .providerId("test123")
                .build();

        // 테스트 구매자 생성
        testBuyer = Buyers.builder()
                .userId(testUser.getId())
                .user(testUser)
                .nameMaskingStatus(false)
                .isDeleted(false)
                .build();

        // 테스트 상품 생성
        testProduct = Products.builder()
                .id(productId)
                .title("신고된 상품")
                .subTitle("문제가 있는 상품")
                .productInfo("상품 정보")
                .contents("상품 내용")
                .price(15000L)
                .stock(50)
                .leadTime((short) 2)
                .build();

        // 테스트 리뷰 생성
        testReview = Reviews.builder()
                .id(reviewId)
                .buyer(testBuyer)
                .product(testProduct)
                .star(1.0)
                .contents("욕설이 포함된 리뷰 내용")
                .summary("불만족")
                .build();

        // 테스트 신고 생성
        testReport = Reports.builder()
                .id(reportId)
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reporter(testBuyer)
                .reason("허위 정보")
                .content("상품 설명과 실제가 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .attachmentUrl("https://example.com/evidence.jpg")
                .build();
    }

    @Test
    @DisplayName("신고 목록 조회 - 전체 조회")
    void getReports_AllReports() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .page(0)
                .size(10)
                .build();

        Reports report2 = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("욕설이 포함된 리뷰입니다")
                .reportStatus(ReportStatus.ON_HOLD)
                .build();

        Page<Reports> reportPage = new PageImpl<>(
                Arrays.asList(testReport, report2),
                PageRequest.of(0, 10),
                2
        );

        given(reportRepository.findReportsWithFilters(
                null, null, null, null, null, PageRequest.of(0, 10)
        )).willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);

        ReportListResponseDto firstReport = result.content().get(0);
        assertThat(firstReport.reportType()).isEqualTo(ReportType.PRODUCT);
        assertThat(firstReport.reason()).isEqualTo("허위 정보");
        assertThat(firstReport.reportStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(firstReport.targetName()).isEqualTo("신고된 상품");

        ReportListResponseDto secondReport = result.content().get(1);
        assertThat(secondReport.reportType()).isEqualTo(ReportType.REVIEW);
        assertThat(secondReport.reason()).isEqualTo("욕설/비방");
        assertThat(secondReport.reportStatus()).isEqualTo(ReportStatus.ON_HOLD);
    }

    @Test
    @DisplayName("신고 목록 조회 - 필터링 (타입별)")
    void getReports_FilterByType() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .reportType(ReportType.PRODUCT)
                .page(0)
                .size(10)
                .build();

        Page<Reports> reportPage = new PageImpl<>(
                List.of(testReport),
                PageRequest.of(0, 10),
                1
        );

        given(reportRepository.findReportsWithFilters(
                ReportType.PRODUCT, null, null, null, null, PageRequest.of(0, 10)
        )).willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reportType()).isEqualTo(ReportType.PRODUCT);
    }

    @Test
    @DisplayName("신고 목록 조회 - 필터링 (상태별)")
    void getReports_FilterByStatus() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .status(ReportStatus.PENDING)
                .page(0)
                .size(10)
                .build();

        Page<Reports> reportPage = new PageImpl<>(
                List.of(testReport),
                PageRequest.of(0, 10),
                1
        );

        given(reportRepository.findReportsWithFilters(
                null, ReportStatus.PENDING, null, null, null, PageRequest.of(0, 10)
        )).willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reportStatus()).isEqualTo(ReportStatus.PENDING);
    }

    @Test
    @DisplayName("신고 목록 조회 - 키워드 검색")
    void getReports_FilterByKeyword() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .keyword("허위")
                .page(0)
                .size(10)
                .build();

        Page<Reports> reportPage = new PageImpl<>(
                List.of(testReport),
                PageRequest.of(0, 10),
                1
        );

        given(reportRepository.findReportsWithFilters(
                null, null, "허위", null, null, PageRequest.of(0, 10)
        )).willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).reason()).contains("허위");
    }

    @Test
    @DisplayName("신고 목록 조회 - 날짜 범위 검색")
    void getReports_FilterByDateRange() {
        // given
        ZonedDateTime startDate = ZonedDateTime.now().minusDays(7);
        ZonedDateTime endDate = ZonedDateTime.now();

        ReportSearchDto searchDto = ReportSearchDto.builder()
                .startDate(startDate)
                .endDate(endDate)
                .page(0)
                .size(10)
                .build();

        ZonedDateTime expectedStartDate = startDate;
        ZonedDateTime expectedEndDate = endDate;

        Page<Reports> reportPage = new PageImpl<>(
                List.of(testReport),
                PageRequest.of(0, 10),
                1
        );

        given(reportRepository.findReportsWithFilters(
                isNull(), isNull(), isNull(), any(ZonedDateTime.class), any(ZonedDateTime.class), any(Pageable.class)
        )).willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(1);
        then(reportRepository).should().findReportsWithFilters(
                null, null, null, expectedStartDate, expectedEndDate, PageRequest.of(0, 10)
        );
    }

    @Test
    @DisplayName("신고 상세 조회 - 성공")
    void getReportDetail_Success() {
        // given
        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(testReport));
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.reportId()).isEqualTo(reportId);
        assertThat(result.reportType()).isEqualTo(ReportType.PRODUCT);
        assertThat(result.reason()).isEqualTo("허위 정보");
        assertThat(result.content()).isEqualTo("상품 설명과 실제가 다릅니다");
        assertThat(result.reportStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(result.reporterName()).isEqualTo("신고자");
        assertThat(result.targetName()).isEqualTo("신고된 상품");
        assertThat(result.targetDescription()).isEqualTo("신고된 상품");
        assertThat(result.attachmentUrl()).isEqualTo("https://example.com/evidence.jpg");
    }

    @Test
    @DisplayName("신고 상세 조회 - 리뷰 신고")
    void getReportDetail_ReviewReport() {
        // given
        Reports reviewReport = Reports.builder()
                .id(reportId)
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("욕설이 포함된 리뷰입니다")
                .reportStatus(ReportStatus.ON_HOLD)
                .adminNote("검토 중")
                .processedByAdminId(adminId)
                .processedAt(ZonedDateTime.now())
                .build();

        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(reviewReport));
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(testReview));

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.reportType()).isEqualTo(ReportType.REVIEW);
        assertThat(result.reason()).isEqualTo("욕설/비방");
        assertThat(result.reportStatus()).isEqualTo(ReportStatus.ON_HOLD);
        assertThat(result.targetName()).isEqualTo("리뷰 #" + reviewId.substring(0, 8));
        assertThat(result.targetDescription()).isEqualTo("욕설이 포함된 리뷰 내용");
        assertThat(result.adminNote()).isEqualTo("검토 중");
        assertThat(result.processedByAdminId()).isEqualTo(adminId);
        assertThat(result.processedAt()).isNotNull();
    }

    @Test
    @DisplayName("신고 상세 조회 - 존재하지 않는 신고")
    void getReportDetail_NotFound() {
        // given
        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminReportService.getReportDetail(reportId))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessage("신고를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신고 상세 조회 - 삭제된 상품")
    void getReportDetail_DeletedProduct() {
        // given
        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(testReport));
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.targetName()).isEqualTo("삭제된 상품");
        assertThat(result.targetDescription()).isEqualTo("상세 정보 없음");
    }

    @Test
    @DisplayName("신고 상태 변경 - 성공")
    void updateReportStatus_Success() {
        // given
        ReportStatusUpdateDto updateDto = ReportStatusUpdateDto.builder()
                .reportStatus(ReportStatus.COMPLETED)
                .adminNote("검토 완료 - 조치 완료")
                .build();

        given(reportRepository.findById(reportId)).willReturn(Optional.of(testReport));

        // when
        adminReportService.updateReportStatus(reportId, updateDto, adminId);

        // then
        assertThat(testReport.getReportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(testReport.getAdminNote()).isEqualTo("검토 완료 - 조치 완료");
        assertThat(testReport.getProcessedByAdminId()).isEqualTo(adminId);
        assertThat(testReport.getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("신고 상태 변경 - 존재하지 않는 신고")
    void updateReportStatus_NotFound() {
        // given
        ReportStatusUpdateDto updateDto = ReportStatusUpdateDto.builder()
                .reportStatus(ReportStatus.COMPLETED)
                .adminNote("검토 완료")
                .build();

        given(reportRepository.findById(reportId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminReportService.updateReportStatus(reportId, updateDto, adminId))
                .isInstanceOf(ReportNotFoundException.class)
                .hasMessage("신고를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신고 상태 변경 - PENDING에서 ON_HOLD로")
    void updateReportStatus_PendingToOnHold() {
        // given
        ReportStatusUpdateDto updateDto = ReportStatusUpdateDto.builder()
                .reportStatus(ReportStatus.ON_HOLD)
                .adminNote("추가 검토 필요")
                .build();

        given(reportRepository.findById(reportId)).willReturn(Optional.of(testReport));

        // when
        adminReportService.updateReportStatus(reportId, updateDto, adminId);

        // then
        assertThat(testReport.getReportStatus()).isEqualTo(ReportStatus.ON_HOLD);
        assertThat(testReport.getAdminNote()).isEqualTo("추가 검토 필요");
        assertThat(testReport.getProcessedByAdminId()).isEqualTo(adminId);
        assertThat(testReport.getProcessedAt()).isNotNull();
        assertThat(testReport.isProcessed()).isTrue();
    }


    @Test
    @DisplayName("신고 대상 이름 조회 - 상품 (존재함)")
    void getTargetName_Product_Exists() {
        // given
        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(testReport));
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.targetName()).isEqualTo("신고된 상품");
        assertThat(result.targetDescription()).isEqualTo("신고된 상품");
    }

    @Test
    @DisplayName("신고 대상 이름 조회 - 리뷰 (존재함)")
    void getTargetName_Review_Exists() {
        // given
        Reports reviewReport = Reports.builder()
                .id(reportId)
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("욕설이 포함된 리뷰입니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(reviewReport));
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(testReview));

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.targetName()).isEqualTo("리뷰 #" + reviewId.substring(0, 8));
        assertThat(result.targetDescription()).isEqualTo("욕설이 포함된 리뷰 내용");
    }

    @Test
    @DisplayName("신고 대상 이름 조회 - 리뷰 (삭제됨)")
    void getTargetName_Review_Deleted() {
        // given
        Reports reviewReport = Reports.builder()
                .id(reportId)
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("욕설이 포함된 리뷰입니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        given(reportRepository.findByIdWithDetails(reportId)).willReturn(Optional.of(reviewReport));
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when
        ReportDetailResponseDto result = adminReportService.getReportDetail(reportId);

        // then
        assertThat(result.targetName()).isEqualTo("리뷰 #" + reviewId.substring(0, 8));
        assertThat(result.targetDescription()).isEqualTo("상세 정보 없음");
    }

    @Test
    @DisplayName("페이징 처리 - 다양한 페이지 크기")
    void getReports_DifferentPageSizes() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .page(0)
                .size(5)
                .build();

        // 5개의 더미 신고 생성
        List<Reports> reports = Arrays.asList(
                createDummyReport("1", ReportType.PRODUCT, ReportStatus.PENDING),
                createDummyReport("2", ReportType.REVIEW, ReportStatus.ON_HOLD),
                createDummyReport("3", ReportType.PRODUCT, ReportStatus.COMPLETED),
                createDummyReport("4", ReportType.REVIEW, ReportStatus.PENDING),
                createDummyReport("5", ReportType.PRODUCT, ReportStatus.ON_HOLD)
        );

        Page<Reports> reportPage = new PageImpl<>(reports, PageRequest.of(0, 5), 20); // 총 20개 중 첫 페이지

        given(reportRepository.findReportsWithFilters(
                null, null, null, null, null, PageRequest.of(0, 5)
        )).willReturn(reportPage);
        given(productRepository.findById(any())).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).hasSize(5);
        assertThat(result.totalElements()).isEqualTo(20);
        assertThat(result.totalPages()).isEqualTo(4);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("빈 검색 결과 처리")
    void getReports_EmptyResult() {
        // given
        ReportSearchDto searchDto = ReportSearchDto.builder()
                .keyword("존재하지않는키워드")
                .page(0)
                .size(10)
                .build();

        Page<Reports> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        given(reportRepository.findReportsWithFilters(
                null, null, "존재하지않는키워드", null, null, PageRequest.of(0, 10)
        )).willReturn(emptyPage);

        // when
        PageResponseDto<ReportListResponseDto> result = adminReportService.getReports(searchDto);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    // === 헬퍼 메서드 ===

    private Reports createDummyReport(String suffix, ReportType reportType, ReportStatus reportStatus) {
        return Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(reportType)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer)
                .reason("신고 사유 " + suffix)
                .content("신고 내용 " + suffix)
                .reportStatus(reportStatus)
                .build();
    }
}