package com.team5.catdogeats.support.domain.reports.service.impl;

import com.team5.catdogeats.products.domain.Products;
import com.team5.catdogeats.products.repository.ProductRepository;
import com.team5.catdogeats.reviews.domain.Reviews;
import com.team5.catdogeats.reviews.repository.ReviewRepository;
import com.team5.catdogeats.support.domain.Reports;
import com.team5.catdogeats.support.domain.dto.PageResponseDto;
import com.team5.catdogeats.support.domain.dto.ReportCreateRequestDto;
import com.team5.catdogeats.support.domain.dto.ReportListResponseDto;
import com.team5.catdogeats.support.domain.enums.ReportStatus;
import com.team5.catdogeats.support.domain.enums.ReportType;
import com.team5.catdogeats.support.repository.ReportRepository;
import com.team5.catdogeats.support.service.impl.ReportServiceImpl;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.repository.BuyerRepository;
import jakarta.persistence.EntityNotFoundException;
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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ReportService 테스트")
class ReportServiceImplTest {

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private BuyerRepository buyerRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReportServiceImpl reportService;

    private Buyers testBuyer;
    private Products testProduct;
    private Reviews testReview;
    private Reports testReport;
    private String buyerId;
    private String productId;
    private String reviewId;

    @BeforeEach
    void setUp() {
        buyerId = UUID.randomUUID().toString();
        productId = UUID.randomUUID().toString();
        reviewId = UUID.randomUUID().toString();

        // 테스트 사용자 생성
        Users testUser = Users.builder()
                .id(buyerId)
                .name("테스트사용자")
                .role(Role.ROLE_BUYER)
                .accountDisable(false)
                .userNameAttribute("test")
                .provider("test")
                .providerId("test123")
                .build();

        // 테스트 구매자 생성
        testBuyer = Buyers.builder()
                .userId(buyerId)
                .user(testUser)
                .nameMaskingStatus(false)
                .isDeleted(false)
                .build();

        // 테스트 상품 생성
        testProduct = Products.builder()
                .id(productId)
                .title("테스트 상품")
                .subTitle("테스트 부제목")
                .productInfo("상품 정보")
                .contents("상품 내용")
                .price(10000L)
                .stock(100)
                .leadTime((short) 3)
                .build();

        // 테스트 리뷰 생성
        testReview = Reviews.builder()
                .id(reviewId)
                .buyer(testBuyer)
                .product(testProduct)
                .star(4.5)
                .contents("좋은 상품입니다")
                .summary("만족")
                .build();

        // 테스트 신고 생성
        testReport = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reporter(testBuyer)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .build();
    }

    @Test
    @DisplayName("상품 신고 생성 - 성공")
    void createReport_Product_Success() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .attachmentUrl("https://example.com/evidence.jpg")
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(testBuyer));
        given(reportRepository.findExistingReport(buyerId, productId)).willReturn(Optional.empty());
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));
        given(reportRepository.save(any(Reports.class))).willReturn(testReport);

        // when
        String reportId = reportService.createReport(request, buyerId);

        // then
        assertThat(reportId).isEqualTo(testReport.getId());
        then(reportRepository).should().save(any(Reports.class));
    }

    @Test
    @DisplayName("리뷰 신고 생성 - 성공")
    void createReport_Review_Success() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .build();

        Reports reviewReport = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(testBuyer));
        given(reportRepository.findExistingReport(buyerId, reviewId)).willReturn(Optional.empty());
        given(reviewRepository.findById(reviewId)).willReturn(Optional.of(testReview));
        given(reportRepository.save(any(Reports.class))).willReturn(reviewReport);

        // when
        String reportId = reportService.createReport(request, buyerId);

        // then
        assertThat(reportId).isEqualTo(reviewReport.getId());
        then(reportRepository).should().save(any(Reports.class));
    }

    @Test
    @DisplayName("신고 생성 - 신고자를 찾을 수 없음")
    void createReport_ReporterNotFound() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.createReport(request, buyerId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("사용자를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신고 생성 - 중복 신고")
    void createReport_DuplicateReport() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(testBuyer));
        given(reportRepository.findExistingReport(buyerId, productId)).willReturn(Optional.of(testReport));

        // when & then
        assertThatThrownBy(() -> reportService.createReport(request, buyerId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("이미 신고한 대상입니다.");
    }

    @Test
    @DisplayName("신고 생성 - 존재하지 않는 상품")
    void createReport_ProductNotFound() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(testBuyer));
        given(reportRepository.findExistingReport(buyerId, productId)).willReturn(Optional.empty());
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.createReport(request, buyerId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("신고 대상 상품을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("신고 생성 - 존재하지 않는 리뷰")
    void createReport_ReviewNotFound() {
        // given
        ReportCreateRequestDto request = ReportCreateRequestDto.builder()
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .build();

        given(buyerRepository.findById(buyerId)).willReturn(Optional.of(testBuyer));
        given(reportRepository.findExistingReport(buyerId, reviewId)).willReturn(Optional.empty());
        given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reportService.createReport(request, buyerId))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("신고 대상 리뷰를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("사용자별 신고 목록 조회 - 성공")
    void getUserReports_Success() {
        // given
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);

        Reports report1 = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reporter(testBuyer)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        Reports report2 = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .reportStatus(ReportStatus.COMPLETED)
                .build();

        Page<Reports> reportPage = new PageImpl<>(Arrays.asList(report1, report2), pageable, 2);

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, pageable))
                .willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, page, size);

        // then
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.page()).isEqualTo(page);
        assertThat(result.size()).isEqualTo(size);

        // 첫 번째 신고 검증
        ReportListResponseDto firstReport = result.content().get(0);
        assertThat(firstReport.reportType()).isEqualTo(ReportType.PRODUCT);
        assertThat(firstReport.reason()).isEqualTo("허위 정보");
        assertThat(firstReport.reportStatus()).isEqualTo(ReportStatus.PENDING);
        assertThat(firstReport.reporterName()).isEqualTo("테스트사용자");
        assertThat(firstReport.targetName()).isEqualTo("테스트 상품");

        // 두 번째 신고 검증 (리뷰 신고)
        ReportListResponseDto secondReport = result.content().get(1);
        assertThat(secondReport.reportType()).isEqualTo(ReportType.REVIEW);
        assertThat(secondReport.reason()).isEqualTo("욕설/비방");
        assertThat(secondReport.reportStatus()).isEqualTo(ReportStatus.COMPLETED);
        assertThat(secondReport.targetName()).isEqualTo("리뷰 #" + reviewId.substring(0, 8));
    }

    @Test
    @DisplayName("사용자별 신고 목록 조회 - 빈 결과")
    void getUserReports_EmptyResult() {
        // given
        int page = 0;
        int size = 10;
        Pageable pageable = PageRequest.of(page, size);
        Page<Reports> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, pageable))
                .willReturn(emptyPage);

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, page, size);

        // then
        assertThat(result.content()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(0);
        assertThat(result.first()).isTrue();
        assertThat(result.last()).isTrue();
        assertThat(result.hasNext()).isFalse();
        assertThat(result.hasPrevious()).isFalse();
    }

    @Test
    @DisplayName("사용자별 신고 목록 조회 - 페이징 테스트")
    void getUserReports_Paging() {
        // given
        int page = 1;
        int size = 5;
        Pageable pageable = PageRequest.of(page, size);

        // 5개의 더미 신고 생성
        List<Reports> reports = List.of(
                createDummyReport("1", ReportType.PRODUCT),
                createDummyReport("2", ReportType.REVIEW),
                createDummyReport("3", ReportType.PRODUCT),
                createDummyReport("4", ReportType.REVIEW),
                createDummyReport("5", ReportType.PRODUCT)
        );

        Page<Reports> reportPage = new PageImpl<>(reports, pageable, 15); // 총 15개 중 두 번째 페이지

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, pageable))
                .willReturn(reportPage);
        given(productRepository.findById(any())).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, page, size);

        // then
        assertThat(result.content()).hasSize(5);
        assertThat(result.totalElements()).isEqualTo(15);
        assertThat(result.totalPages()).isEqualTo(3);
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.first()).isFalse();
        assertThat(result.last()).isFalse();
        assertThat(result.hasNext()).isTrue();
        assertThat(result.hasPrevious()).isTrue();
    }

    @Test
    @DisplayName("신고 대상 이름 조회 - 상품 (존재함)")
    void getTargetName_Product_Exists() {
        // given
        Reports productReport = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reporter(testBuyer)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        Page<Reports> reportPage = new PageImpl<>(List.of(productReport), PageRequest.of(0, 10), 1);

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, PageRequest.of(0, 10)))
                .willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.of(testProduct));

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, 0, 10);

        // then
        assertThat(result.content().get(0).targetName()).isEqualTo("테스트 상품");
    }

    @Test
    @DisplayName("신고 대상 이름 조회 - 상품 (삭제됨)")
    void getTargetName_Product_Deleted() {
        // given
        Reports productReport = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.PRODUCT)
                .targetId(productId)
                .reporter(testBuyer)
                .reason("허위 정보")
                .content("상품 설명이 실제와 다릅니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        Page<Reports> reportPage = new PageImpl<>(List.of(productReport), PageRequest.of(0, 10), 1);

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, PageRequest.of(0, 10)))
                .willReturn(reportPage);
        given(productRepository.findById(productId)).willReturn(Optional.empty());

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, 0, 10);

        // then
        assertThat(result.content().get(0).targetName()).isEqualTo("삭제된 상품");
    }

    @Test
    @DisplayName("신고 대상 이름 조회 - 리뷰")
    void getTargetName_Review() {
        // given
        Reports reviewReport = Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(ReportType.REVIEW)
                .targetId(reviewId)
                .reporter(testBuyer)
                .reason("욕설/비방")
                .content("리뷰에 욕설이 포함되어 있습니다")
                .reportStatus(ReportStatus.PENDING)
                .build();

        Page<Reports> reportPage = new PageImpl<>(List.of(reviewReport), PageRequest.of(0, 10), 1);

        given(reportRepository.findByReporterIdOrderByCreatedAtDesc(buyerId, PageRequest.of(0, 10)))
                .willReturn(reportPage);

        // when
        PageResponseDto<ReportListResponseDto> result = reportService.getUserReports(buyerId, 0, 10);

        // then
        assertThat(result.content().get(0).targetName()).isEqualTo("리뷰 #" + reviewId.substring(0, 8));
    }

    // === 헬퍼 메서드 ===

    private Reports createDummyReport(String suffix, ReportType reportType) {
        return Reports.builder()
                .id(UUID.randomUUID().toString())
                .reportType(reportType)
                .targetId(UUID.randomUUID().toString())
                .reporter(testBuyer)
                .reason("신고 사유 " + suffix)
                .content("신고 내용 " + suffix)
                .reportStatus(ReportStatus.PENDING)
                .build();
    }
}