package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.global.annotation.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.storage.service.InquiryFileService;
import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryMessageType;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryEscalationService;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import com.team5.catdogeats.support.domain.inquiry.util.InquiryUrgencyEscalationUtil;
import com.team5.catdogeats.support.domain.inquiry.util.InquiryUrgentLevelUtil;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final InquiryFileService inquiryFileService;


    @Override
    @JpaTransactional(readOnly = true)
    public Page<InquiryListResponseDTO> getUserInquiries(String providerId, Pageable pageable) {
        Page<Inquires> inquiries = inquiryRepository.findByUserProviderIdOrderByCreatedAtDesc(providerId, pageable);
        return inquiries.map(InquiryListResponseDTO::forUser);
    }

    // 사용자용 상세 조회
    @Override
    @JpaTransactional(readOnly = true)
    public InquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String providerId) {
        // 한 번의 쿼리로 문의 조회 + 권한 검증
        Inquires inquiry = inquiryRepository.findByIdAndUserProviderId(inquiryId, providerId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없거나 접근 권한이 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(inquiry);
        List<InquiryMessageDTO> messages = getInquiryMessages(rootInquiry.getId());
        List<InquiryAttachmentDTO> attachments = inquiryFileService.getInquiryThreadAttachments(rootInquiry.getId());

        return InquiryDetailResponseDTO.forUser(rootInquiry, messages, attachments);
    }



    @Override
    @JpaTransactional
    public InquiryResponseDTO createInquiry(String providerId, InquiryCreateRequestDTO request) {

        // providerId로 사용자 조회 (provider + providerId 조합)
        Users user = getUserByProviderId(providerId);

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
    public InquiryResponseDTO createUserFollowup(String inquiryId, String providerId, String content) {
        // ✅ 서비스에서 검증 및 DTO 생성 처리
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("내용은 필수입니다.");
        }

        // ✅ DTO 생성을 서비스에서 처리
        InquiryRequestDTO request = InquiryRequestDTO.forContent(content);

        Inquires targetInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);
        validateUserAccess(rootInquiry, providerId);
        validateInquiryNotClosed(rootInquiry);

        Users user = getUserByProviderId(providerId);

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
                .content(request.content())
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
    public InquiryResponseDTO closeInquiryByUser(String inquiryId, String providerId) {
        Inquires targetInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);
        validateUserAccess(rootInquiry, providerId);

        if (rootInquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                rootInquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("이미 종료된 문의입니다.");
        }

        rootInquiry.setInquiryStatus(InquiryStatus.CLOSED);

        return InquiryResponseDTO.closed(rootInquiry);
    }

    @Override
    @JpaTransactional
    public Page<InquiryListResponseDTO> getAllInquiries(Pageable pageable) {
        Page<Inquires> inquiries = inquiryRepository.findAllInquiriesOrderByCreatedAtDesc(pageable);

        // 🔥 핵심: 간단한 배치 처리
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

    // 간단한 배치 처리 메서드 (private)
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

    @Override
    @JpaTransactional(readOnly = true)
    public InquiryDetailResponseDTO getInquiryDetailForAdmin(String inquiryId) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        List<InquiryMessageDTO> messages = getInquiryMessages(inquiry.getId());

        List<InquiryAttachmentDTO> attachments = inquiryFileService.getInquiryThreadAttachments(inquiry.getId());

        return InquiryDetailResponseDTO.forAdmin(inquiry, messages, attachments);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createAdminReply(String inquiryId, String adminId, String content) {
        // ✅ 서비스에서 검증 및 DTO 생성 처리
        if (content == null || content.trim().isEmpty()) {
            throw new IllegalArgumentException("답변 내용은 필수입니다.");
        }

        // ✅ DTO 생성을 서비스에서 처리
        InquiryRequestDTO request = InquiryRequestDTO.forContent(content);

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
                .admins(null) // Todo: admin쪽 엔티티 연결 필요
                .title("Re: " + rootInquiry.getTitle())
                .content(request.content())
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
        // ✅ 서비스에서 검증 및 DTO 생성 처리
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("강제 종료 시 사유는 필수입니다.");
        }

        // ✅ DTO 생성을 서비스에서 처리
        InquiryRequestDTO request = InquiryRequestDTO.forClose(reason);

        Inquires targetInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        Inquires rootInquiry = findRootInquiry(targetInquiry);

        if (rootInquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                rootInquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("이미 종료된 문의입니다.");
        }

        rootInquiry.setInquiryStatus(InquiryStatus.FORCE_CLOSED);

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


    private void validateInquiryNotClosed(Inquires inquiry) {
        if (inquiry.getInquiryStatus() == InquiryStatus.CLOSED ||
                inquiry.getInquiryStatus() == InquiryStatus.FORCE_CLOSED) {
            throw new IllegalStateException("종료된 문의에는 답글을 등록할 수 없습니다.");
        }
    }


    private void validateUserAccess(Inquires inquiry, String providerId) {
        String userId = getUserIdByProviderId(providerId);
        if (!inquiry.getUsers().getId().equals(userId)) {
            throw new AccessDeniedException("해당 문의에 대한 접근 권한이 없습니다.");
        }
    }

    private List<InquiryMessageDTO> getInquiryMessages(String inquiryId) {
        List<Inquires> replies = inquiryRepository.findByParentIdOrderByCreatedAtAsc(inquiryId);
        return replies.stream()
                .map(InquiryMessageDTO::from)
                .collect(Collectors.toList());
    }

    private Inquires findRootInquiry(Inquires inquiry) {
        Inquires current = inquiry;
        while (current.getParent() != null) {
            current = current.getParent();
        }
        return current;
    }

    private Users getUserByProviderId(String providerId) {
        // 모든 provider에서 providerId로 검색
        // TODO: JWT에서 provider 정보도 함께 전달받도록 개선 필요
        return userRepository.findByProviderAndProviderId("google", providerId)
                .or(() -> userRepository.findByProviderAndProviderId("kakao", providerId))
                .or(() -> userRepository.findByProviderAndProviderId("naver", providerId))
                .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다: " + providerId));
    }

    // providerId로 사용자의 실제 ID(PK) 조회
    private String getUserIdByProviderId(String providerId) {
        return getUserByProviderId(providerId).getId();
    }


    // 🆕 추가할 메서드 1: 파일과 함께 문의 생성
    public InquiryResponseDTO createInquiryWithFiles(InquiryCreateRequestDTO request,
                                                     MultipartFile[] imageFiles,
                                                     String providerId) {
        try {
            // 1. 기본 문의 생성
            InquiryResponseDTO response = createInquiry(providerId, request);

            // 2. 파일 업로드 (같은 트랜잭션 내에서)
            if (imageFiles != null && imageFiles.length > 0) {
                inquiryFileService.uploadUserImages(response.inquiryId(), imageFiles);
            }

            return response;
        } catch (EntityNotFoundException | IllegalArgumentException e) {
            throw e; // 그대로 전파
        } catch (RuntimeException e) {
            log.error("문의 생성 실패 - providerId: {}", providerId, e);
            throw new IllegalStateException("문의 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 🆕 추가할 메서드 2: 파일과 함께 사용자 답글 생성
    @Override
    @JpaTransactional
    public InquiryResponseDTO createUserFollowupWithFiles(String inquiryId,
                                                          String content,
                                                          MultipartFile[] imageFiles,
                                                          String providerId) {
        try {
            InquiryRequestDTO request = InquiryRequestDTO.forContent(content);
            InquiryResponseDTO response = createUserFollowup(inquiryId, providerId, request.content());

            if (imageFiles != null && imageFiles.length > 0) {
                inquiryFileService.uploadUserImages(response.inquiryId(), imageFiles);
            }

            return response;
        } catch (EntityNotFoundException | AccessDeniedException | IllegalArgumentException | IllegalStateException e) {
            throw e; // 그대로 전파
        } catch (RuntimeException e) {
            log.error("파일과 함께 답글 생성 실패 - inquiryId: {}, providerId: {}", inquiryId, providerId, e);
            throw new IllegalStateException("답글 생성 중 오류가 발생했습니다.", e);
        }
    }

    // 🆕 추가할 메서드 3: 파일과 함께 관리자 답변 생성
    @Override
    @JpaTransactional
    public InquiryResponseDTO createAdminReplyWithFiles(String inquiryId,
                                                        String content,
                                                        MultipartFile[] imageFiles,
                                                        MultipartFile[] documentFiles,
                                                        String adminId) {
        try {
            InquiryRequestDTO inquiryRequest = InquiryRequestDTO.forContent(content);
            InquiryResponseDTO response = createAdminReply(inquiryId, adminId, inquiryRequest.content());

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
}