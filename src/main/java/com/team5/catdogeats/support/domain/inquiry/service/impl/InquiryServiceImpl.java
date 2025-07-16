package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.admins.domain.Admins;
import com.team5.catdogeats.admins.repository.AdminRepository;
import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.storage.service.InquiryFileService;
import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryMessageType;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryType;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryAttachmentDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquiryCreateRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.request.InquirySearchRequestDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryDetailResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryListResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryMessageDTO;
import com.team5.catdogeats.support.domain.inquiry.dto.response.InquiryResponseDTO;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryEscalationService;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import com.team5.catdogeats.support.domain.inquiry.util.InquiryUrgencyEscalationUtil;
import com.team5.catdogeats.support.domain.inquiry.util.InquiryUrgentLevelUtil;
import com.team5.catdogeats.support.domain.inquiry.util.InquirySearchUtil;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final InquiryEscalationService escalationService;
    private final AdminRepository adminRepository;
    private final InquiryFileService inquiryFileService;

    @Override
    @JpaTransactional(readOnly = true)
    public Page<InquiryListResponseDTO> getUserInquiries(String provider, String providerId, Pageable pageable) {
        // 1. provider + providerId로 사용자 조회
        Users user = getUserByProviderAndProviderId(provider, providerId);

        // 2. 사용자 ID로 문의 조회
        Page<Inquires> inquiries = inquiryRepository.findByUsersIdOrderByCreatedAtDesc(user.getId(), pageable);
        return inquiries.map(InquiryListResponseDTO::forUser);
    }

    @Override
    @JpaTransactional(readOnly = true)
    public InquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String provider, String providerId) {
        // 1. provider + providerId로 사용자 조회
        Users user = getUserByProviderAndProviderId(provider, providerId);

        // 2. 문의 조회 및 권한 검증
        Inquires rootInquiry = inquiryRepository.findRootInquiryWithRepliesByIdAndUserId(inquiryId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없거나 접근 권한이 없습니다: " + inquiryId));

        // 답글들을 DTO로 변환 (메모리에서 처리, 추가 DB 쿼리 없음)
        List<InquiryMessageDTO> messages = rootInquiry.getReplies().stream()
                .sorted((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .map(InquiryMessageDTO::from)
                .collect(Collectors.toList());

        // 첨부파일들 조회
        List<InquiryAttachmentDTO> attachments = inquiryFileService.getInquiryThreadAttachments(rootInquiry.getId());

        return InquiryDetailResponseDTO.forUser(rootInquiry, messages, attachments);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createInquiry(String provider, String providerId, InquiryCreateRequestDTO request) {
        // provider + providerId로 사용자 조회
        Users user = getUserByProviderAndProviderId(provider, providerId);

        // 주문 정보 조회 (있는 경우)
        Orders order = null;
        String orderId = request.orderId();
        if (orderId != null && !orderId.trim().isEmpty() &&
                !orderId.equalsIgnoreCase("null") && !orderId.equals("undefined")) {
            order = orderRepository.findById(orderId.trim())
                    .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + orderId));
        }

        InquiryUrgentLevel defaultUrgentLevel = InquiryUrgentLevelUtil.getDefaultUrgentLevel(request.inquiryType());

        Inquires inquiry = Inquires.builder()
                .users(user)
                .title(request.title())
                .content(request.content())
                .inquiryType(request.inquiryType())
                .inquiryReceiveMethod(request.inquiryReceiveMethod())
                .inquiryStatus(InquiryStatus.PENDING)
                .inquiryUrgentLevel(defaultUrgentLevel)
                .inquiryMessageType(InquiryMessageType.QUESTION)
                .orders(order)
                .build();

        // 문의 저장
        Inquires savedInquiry = inquiryRepository.save(inquiry);
        return InquiryResponseDTO.created(savedInquiry);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createUserFollowup(String inquiryId, String provider, String providerId, String content) {
        // 입력 검증
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }

        // 1. provider + providerId로 사용자 조회
        Users user = getUserByProviderAndProviderId(provider, providerId);

        // 2. 권한 검증 + 조회를 한번에
        Inquires targetInquiry = inquiryRepository.findByIdAndUsersId(inquiryId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없거나 접근 권한이 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);
        validateInquiryNotClosed(rootInquiry);

        InquiryMessageType messageType;
        InquiryStatus newStatus;
        InquiryStatus rootNewStatus;

        switch (rootInquiry.getInquiryStatus()) {
            case PENDING:
                newStatus = InquiryStatus.PENDING;
                rootNewStatus = InquiryStatus.PENDING;
                messageType = InquiryMessageType.QUESTION;
                break;
            case ANSWERED:
                newStatus = InquiryStatus.FOLLOWUP;
                rootNewStatus = InquiryStatus.FOLLOWUP;
                messageType = InquiryMessageType.USER_FOLLOWUP;
                break;
            case FOLLOWUP:
                newStatus = InquiryStatus.FOLLOWUP;
                rootNewStatus = InquiryStatus.FOLLOWUP;
                messageType = InquiryMessageType.USER_FOLLOWUP;
                break;
            default:
                throw new IllegalStateException("알 수 없는 문의 상태입니다: " + rootInquiry.getInquiryStatus());
        }

        Inquires followup = Inquires.builder()
                .parent(rootInquiry)
                .users(user)
                .admins(null)
                .title("Re: " + rootInquiry.getTitle())
                .content(content.trim())
                .inquiryType(rootInquiry.getInquiryType())
                .inquiryReceiveMethod(rootInquiry.getInquiryReceiveMethod())
                .inquiryStatus(newStatus)
                .inquiryUrgentLevel(rootInquiry.getInquiryUrgentLevel())
                .inquiryMessageType(messageType)
                .orders(rootInquiry.getOrders())
                .build();

        rootInquiry.setInquiryStatus(rootNewStatus);
        Inquires savedFollowup = inquiryRepository.save(followup);

        return InquiryResponseDTO.followupAdded(savedFollowup);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO closeInquiryByUser(String inquiryId, String provider, String providerId) {
        // 1. provider + providerId로 사용자 조회
        Users user = getUserByProviderAndProviderId(provider, providerId);

        // 2. 권한 검증 + 조회를 한번에
        Inquires targetInquiry = inquiryRepository.findByIdAndUsersId(inquiryId, user.getId())
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없거나 접근 권한이 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);

        if (rootInquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                rootInquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("이미 종료된 문의입니다.");
        }

        rootInquiry.setInquiryStatus(InquiryStatus.CLOSED);
        return InquiryResponseDTO.closed(rootInquiry);
    }

    // 파일과 함께 문의 생성
    @Override
    @JpaTransactional
    public InquiryResponseDTO createInquiryWithFiles(InquiryCreateRequestDTO request,
                                                     MultipartFile[] imageFiles,
                                                     String provider,
                                                     String providerId) {
        try {
            // 1. 기본 문의 생성
            InquiryResponseDTO response = createInquiry(provider, providerId, request);

            // 2. 파일 업로드 (같은 트랜잭션 내에서)
            if (imageFiles != null && imageFiles.length > 0) {
                inquiryFileService.uploadUserImages(response.inquiryId(), imageFiles);
            }

            return response;
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            throw e; // 그대로 전파
        } catch (RuntimeException e) {
            log.error("문의 생성 실패 - provider: {}, providerId: {}", provider, providerId, e);
            throw new IllegalStateException("문의 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 파일과 함께 사용자 답글 생성
    @Override
    @JpaTransactional
    public InquiryResponseDTO createUserFollowupWithFiles(String inquiryId,
                                                          String content,
                                                          MultipartFile[] imageFiles,
                                                          String provider,
                                                          String providerId) {
        try {
            // 기존 메서드 호출 (직접 content 전달)
            InquiryResponseDTO response = createUserFollowup(inquiryId, provider, providerId, content);

            if (imageFiles != null && imageFiles.length > 0) {
                inquiryFileService.uploadUserImages(response.inquiryId(), imageFiles);
            }

            return response;
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
            throw e; // 그대로 전파
        } catch (RuntimeException e) {
            log.error("파일과 함께 답글 생성 실패 - inquiryId: {}, provider: {}, providerId: {}", inquiryId, provider, providerId, e);
            throw new IllegalStateException("답글 생성 중 오류가 발생했습니다.", e);
        }
    }

    // === 관리자 전용 기능들 (변경 없음) ===

    @Override
    @JpaTransactional
    public Page<InquiryListResponseDTO> getAllInquiries(Pageable pageable) {
        Page<Inquires> inquiries = inquiryRepository.findAllInquiriesOrderByCreatedAtDesc(pageable);

        // 간단한 배치 처리
        updateUrgencyBatch(inquiries.getContent());

        List<InquiryListResponseDTO> content = new ArrayList<>();
        for (int i = 0; i < inquiries.getContent().size(); i++) {
            Inquires inquiry = inquiries.getContent().get(i);
            int sequenceNumber = (int) (inquiries.getTotalElements() -
                    (pageable.getPageNumber() * pageable.getPageSize()) - i);
            content.add(InquiryListResponseDTO.forAdmin(inquiry, sequenceNumber));
        }

        return new PageImpl<>(content, pageable, inquiries.getTotalElements());
    }

    @Override
    @JpaTransactional(readOnly = true)
    public InquiryDetailResponseDTO getInquiryDetailForAdmin(String inquiryId) {
        // 🎯 핵심: 1차 쿼리로 루트 문의 + 모든 답글을 한번에!
        Inquires rootInquiry = inquiryRepository.findRootInquiryWithRepliesById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        // 답글들을 DTO로 변환 (메모리에서 처리, 추가 DB 쿼리 없음)
        List<InquiryMessageDTO> messages = rootInquiry.getReplies().stream()
                .sorted((r1, r2) -> r1.getCreatedAt().compareTo(r2.getCreatedAt()))
                .map(InquiryMessageDTO::from)
                .collect(Collectors.toList());

        // 2차 쿼리: 첨부파일들만 별도 조회 (개선 예정)
        List<InquiryAttachmentDTO> attachments = inquiryFileService.getInquiryThreadAttachments(rootInquiry.getId());

        return InquiryDetailResponseDTO.forAdmin(rootInquiry, messages, attachments);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createAdminReply(String inquiryId, String adminId, String content) {
        // 입력 검증
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("답변 내용은 필수입니다.");
        }

        // adminId로 Admin 엔티티 조회
        Admins admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다: " + adminId));

        Inquires targetInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);
        validateInquiryNotClosed(rootInquiry);

        InquiryMessageType messageType;
        InquiryStatus newStatus;
        InquiryStatus rootNewStatus;

        switch (rootInquiry.getInquiryStatus()) {
            case PENDING:
                messageType = InquiryMessageType.ANSWER;
                newStatus = InquiryStatus.ANSWERED;
                rootNewStatus = InquiryStatus.ANSWERED;
                break;
            case ANSWERED:
            case FOLLOWUP:
                messageType = InquiryMessageType.ADMIN_FOLLOWUP;
                newStatus = InquiryStatus.FOLLOWUP;
                rootNewStatus = InquiryStatus.FOLLOWUP;
                break;
            default:
                throw new IllegalStateException("알 수 없는 문의 상태입니다: " + rootInquiry.getInquiryStatus());
        }

        Inquires reply = Inquires.builder()
                .parent(rootInquiry)
                .users(rootInquiry.getUsers())
                .admins(admin)
                .title("Re: " + rootInquiry.getTitle())
                .content(content.trim())
                .inquiryType(rootInquiry.getInquiryType())
                .inquiryReceiveMethod(rootInquiry.getInquiryReceiveMethod())
                .inquiryStatus(newStatus)
                .inquiryUrgentLevel(rootInquiry.getInquiryUrgentLevel())
                .inquiryMessageType(messageType)
                .orders(rootInquiry.getOrders())
                .build();

        rootInquiry.setInquiryStatus(rootNewStatus);
        Inquires savedReply = inquiryRepository.save(reply);

        return InquiryResponseDTO.replied(savedReply);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO closeInquiryByAdmin(String inquiryId, String adminId, String reason) {
        // 입력 검증
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("강제 종료 시 사유는 필수입니다.");
        }

        // adminId로 Admin 엔티티 조회
        Admins admin = adminRepository.findById(adminId)
                .orElseThrow(() -> new EntityNotFoundException("관리자를 찾을 수 없습니다: " + adminId));

        Inquires targetInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);

        if (rootInquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                rootInquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("이미 종료된 문의입니다.");
        }

        rootInquiry.setInquiryStatus(InquiryStatus.FORCE_CLOSED);
        rootInquiry.setAdmins(admin);

        // 강제 종료 사유는 별도 로깅 또는 필요시 엔티티에 추가 필드로 저장
        log.info("문의 강제 종료 - inquiryId: {}, adminId: {}, reason: {}",
                inquiryId, adminId, reason);

        inquiryRepository.save(rootInquiry);
        return InquiryResponseDTO.closed(rootInquiry);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO updateUrgentLevel(String inquiryId, InquiryUrgentLevel urgentLevel) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        inquiry.setInquiryUrgentLevel(urgentLevel);
        return InquiryResponseDTO.from(inquiry, "긴급도가 성공적으로 수정되었습니다.");
    }

    // 파일과 함께 관리자 답변 생성
    @Override
    @JpaTransactional
    public InquiryResponseDTO createAdminReplyWithFiles(String inquiryId,
                                                        String content,
                                                        MultipartFile[] imageFiles,
                                                        MultipartFile[] documentFiles,
                                                        String adminId) {
        try {
            // 기존 메서드 호출 (직접 content 전달)
            InquiryResponseDTO response = createAdminReply(inquiryId, adminId, content);

            // 파일 업로드 처리
            if ((imageFiles != null && imageFiles.length > 0) ||
                    (documentFiles != null && documentFiles.length > 0)) {
                inquiryFileService.uploadAdminFiles(response.inquiryId(), imageFiles, documentFiles);
            }

            return response;
        } catch (RuntimeException e) {
            log.error("관리자 답변 생성 실패 - inquiryId: {}, adminId: {}", inquiryId, adminId, e);
            throw new IllegalStateException("관리자 답변 생성 중 오류가 발생했습니다.", e);
        }
    }

    @Override
    @JpaTransactional(readOnly = true)
    public Page<InquiryListResponseDTO> searchInquiries(InquirySearchRequestDTO searchRequest, Pageable pageable) {
        log.info("문의 검색 시작 - 조건: {}", searchRequest.getSearchSummary());

        // 동적 쿼리 생성
        Specification<Inquires> spec = InquirySearchUtil.searchInquiries(searchRequest);

        // 검색 실행
        Page<Inquires> inquiries = inquiryRepository.findAll(spec, pageable);

        // 🔥 핵심: 긴급도 배치 업데이트
        updateUrgencyBatch(inquiries.getContent());

        // DTO 변환 (시퀀스 넘버 계산)
        List<InquiryListResponseDTO> content = new ArrayList<>();
        for (int i = 0; i < inquiries.getContent().size(); i++) {
            Inquires inquiry = inquiries.getContent().get(i);
            int sequenceNumber = (int) (inquiries.getTotalElements() -
                    (pageable.getPageNumber() * pageable.getPageSize()) - i);
            content.add(InquiryListResponseDTO.forAdmin(inquiry, sequenceNumber));
        }

        log.info("문의 검색 완료 - 결과: {}건, 전체: {}건",
                content.size(), inquiries.getTotalElements());

        return new PageImpl<>(content, pageable, inquiries.getTotalElements());
    }

    @Override
    @JpaTransactional(readOnly = true)
    public Page<InquiryListResponseDTO> getAllInquiriesWithSearchAndPaging(
            String keyword, InquiryStatus status, InquiryType type, InquiryUrgentLevel urgentLevel,
            LocalDate startDate, LocalDate endDate, int page, int size, String sort, String direction) {

        // Pageable 객체 생성
        Sort.Direction sortDirection = direction.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sort));

        // 1. 검색 조건 검증 (Service 책임)
        validateSearchParameters(keyword, startDate, endDate, pageable);

        // 2. 검색 조건 DTO 생성 (Service 책임)
        InquirySearchRequestDTO searchRequest = createSearchRequest(
                keyword, status, type, urgentLevel, startDate, endDate
        );

        // 3. 로깅 (Service 책임)
        log.info("관리자 문의 조회 시작 - 검색조건: {}, 페이지: {}/{}",
                searchRequest.getSearchSummary(), pageable.getPageNumber(), pageable.getPageSize());

        // 4. 검색 조건이 없으면 기존 메서드 사용 (성능 최적화)
        if (searchRequest == null || !searchRequest.hasSearchConditions()) {
            log.info("검색 조건 없음 - 전체 조회 실행");
            return getAllInquiries(pageable);
        }

        // 5. 검색 조건이 있으면 동적 쿼리 사용
        return searchInquiries(searchRequest, pageable);
    }

    // =================================
    // Private Helper Methods
    // =================================

    /**
     * provider + providerId로 사용자 조회 (개선된 버전)
     */
    private Users getUserByProviderAndProviderId(String provider, String providerId) {
        return userRepository.findByProviderAndProviderId(provider, providerId)
                .orElseThrow(() -> new EntityNotFoundException(
                        String.format("사용자를 찾을 수 없습니다: provider=%s, providerId=%s", provider, providerId)));
    }

    /**
     * 간단한 배치 처리 메서드
     */
    private void updateUrgencyBatch(List<Inquires> inquiries) {
        for (Inquires inquiry : inquiries) {
            InquiryUrgentLevel currentLevel = inquiry.getInquiryUrgentLevel();
            InquiryUrgentLevel escalatedLevel = InquiryUrgencyEscalationUtil.calculateEscalatedUrgency(
                    inquiry.getCreatedAt(),
                    currentLevel,
                    inquiry.getInquiryStatus(),
                    inquiry.getInquiryType()
            );

            // 긴급도가 변경된 경우에만 업데이트 (더티체킹으로 자동 저장)
            if (!escalatedLevel.equals(currentLevel)) {
                inquiry.setInquiryUrgentLevel(escalatedLevel);
                log.debug("긴급도 업데이트 - ID: {}, {} -> {}",
                        inquiry.getId(), currentLevel.getDisplayName(), escalatedLevel.getDisplayName());
            }
        }
    }

    /**
     * 문의가 종료되지 않았는지 검증
     */
    private void validateInquiryNotClosed(Inquires inquiry) {
        if (inquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                inquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("종료된 문의에는 답글을 등록할 수 없습니다.");
        }
    }

    /**
     * 루트 문의 조회 (재귀적으로 부모를 따라 올라감)
     */
    private Inquires findRootInquiry(Inquires inquiry) {
        Inquires current = inquiry;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    /**
     * 검색 파라미터 검증
     */
    private void validateSearchParameters(String keyword, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // 키워드 길이 검증
        if (keyword != null && keyword.trim().length() > 100) {
            throw new IllegalArgumentException("검색 키워드는 100자를 초과할 수 없습니다.");
        }

        // 날짜 범위 검증
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("시작일은 종료일보다 이전이어야 합니다.");
        }

        // 미래 날짜 검증
        LocalDate today = LocalDate.now();
        if (startDate != null && startDate.isAfter(today)) {
            throw new IllegalArgumentException("시작일은 현재 날짜보다 이전이어야 합니다.");
        }

        // 날짜 범위 제한 (예: 1년 이내)
        if (startDate != null && endDate != null && startDate.plusYears(1).isBefore(endDate)) {
            throw new IllegalArgumentException("검색 기간은 1년을 초과할 수 없습니다.");
        }

        // 페이징 검증
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("페이지 크기는 100을 초과할 수 없습니다.");
        }
    }

    /**
     * 검색 조건 DTO 생성
     */
    private InquirySearchRequestDTO createSearchRequest(String keyword, InquiryStatus status,
                                                        InquiryType type, InquiryUrgentLevel urgentLevel,
                                                        LocalDate startDate, LocalDate endDate) {
        // 키워드 정리 (공백 제거, 빈 문자열을 null로 변환)
        String cleanKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;

        return new InquirySearchRequestDTO(
                cleanKeyword, status, type, urgentLevel, startDate, endDate
        );
    }
}