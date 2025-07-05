package com.team5.catdogeats.support.domain.inquiry.service.impl;

import com.team5.catdogeats.global.config.JpaTransactional;
import com.team5.catdogeats.orders.domain.Orders;
import com.team5.catdogeats.orders.repository.OrderRepository;
import com.team5.catdogeats.support.domain.Inquires;
import com.team5.catdogeats.support.domain.enums.InquiryStatus;
import com.team5.catdogeats.support.domain.enums.InquiryUrgentLevel;
import com.team5.catdogeats.support.domain.inquiry.dto.*;
import com.team5.catdogeats.support.domain.inquiry.repository.InquiryRepository;
import com.team5.catdogeats.support.domain.inquiry.service.InquiryService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InquiryServiceImpl implements InquiryService {

    private final InquiryRepository inquiryRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @Override
    @JpaTransactional(readOnly = true)
    public Page<UserInquiryListResponseDTO> getUserInquiries(String providerId, Pageable pageable) {
        String userId = getUserIdByProviderId(providerId);
        Page<Inquires> inquiries = inquiryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        return inquiries.map(UserInquiryListResponseDTO::from);
    }

    // 사용자용 상세 조회
    @Override
    @JpaTransactional(readOnly = true)
    public UserInquiryDetailResponseDTO getUserInquiryDetail(String inquiryId, String providerId) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        validateUserAccess(inquiry, providerId);

        // 답변 조회 - 단일 답변으로 변경
        List<InquiryReplyResponseDTO> replies = new ArrayList<>();

        // 답변이 있는 경우에만 조회 (상태가 ANSWERED인 경우)
        if (inquiry.getInquiryStatus() == InquiryStatus.ANSWERED) {
            Optional<Inquires> replyOptional = inquiryRepository.findByParent_Id(inquiry.getId());
            if (replyOptional.isPresent()) {
                replies.add(InquiryReplyResponseDTO.from(replyOptional.get()));
            }
        }

        String userId = getUserIdByProviderId(providerId);
        log.info("문의 상세 조회 - inquiryId: {}, userId: {}, hasReplies: {}",
                inquiryId, userId, !replies.isEmpty());

        List<InquiryAttachmentDTO> attachedImages = new ArrayList<>();
        return UserInquiryDetailResponseDTO.from(inquiry, replies, attachedImages);
    }

    private void validateUserAccess(Inquires inquiry, String providerId) {
        String userId = getUserIdByProviderId(providerId);
        if (!inquiry.getUsers().getId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("해당 문의에 대한 접근 권한이 없습니다.");
        }
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createInquiry(String providerId, InquiryCreateRequestDTO request) {

        // providerId로 사용자 조회 (provider + providerId 조합)
        Users user = getUserByProviderId(providerId);

        // 주문 정보 조회 (있는 경우)
        Orders order = null;
        String orderId = request.orderId();

        // null, 빈 문자열, "null" 문자열, 공백만 있는 경우 모두 제외
        if (orderId != null &&
                !orderId.trim().isEmpty() &&
                !orderId.equalsIgnoreCase("null") &&
                !orderId.equals("undefined")) {

            order = orderRepository.findById(orderId.trim())
                    .orElseThrow(() -> new EntityNotFoundException("주문을 찾을 수 없습니다: " + orderId));
        }

        // 문의 엔티티 생성
        Inquires inquiry = Inquires.builder()
                .users(user)
                .title(request.title())
                .content(request.content())
                .inquiryType(request.inquiryType())
                .inquiryReceiveMethod(request.inquiryReceiveMethod())
                .inquiryStatus(InquiryStatus.PENDING)
                .inquiryUrgentLevel(InquiryUrgentLevel.MEDIUM) // 기본값
                .orders(order)  // null이어도 괜찮음
                .build();

        // 문의 저장
        Inquires savedInquiry = inquiryRepository.save(inquiry);
        log.info("문의 등록 - userId: {}, inquiryType: {}, hasOrderId: {}, titleLength: {}",
                user.getId(),
                request.inquiryType(),
                orderId != null,
                request.title().length());

        return InquiryResponseDTO.created(savedInquiry);
    }

    @Override
    @JpaTransactional(readOnly = true)
    public Page<InquiryListResponseDTO> getAllInquiries(Pageable pageable) {
        Page<Inquires> inquiries = inquiryRepository.findAllInquiriesOrderByCreatedAtDesc(pageable);

        // 페이지 정보를 이용해서 순번 계산 (getUserInquiries와 동일한 방식)
        List<InquiryListResponseDTO> content = new ArrayList<>();
        for (int i = 0; i < inquiries.getContent().size(); i++) {
            Inquires inquiry = inquiries.getContent().get(i);
            int sequenceNumber = (int) (inquiries.getTotalElements() - (pageable.getPageNumber() * pageable.getPageSize()) - i);
            content.add(InquiryListResponseDTO.from(inquiry, sequenceNumber));
        }

        return new PageImpl<>(content, pageable, inquiries.getTotalElements());
    }

    @Override
    @JpaTransactional(readOnly = true)
    public InquiryDetailResponseDTO getInquiryDetailForAdmin(String inquiryId) {
        Inquires inquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        return buildInquiryDetailResponse(inquiry);
    }

    @Override
    @JpaTransactional
    public InquiryResponseDTO createReply(String inquiryId, String adminId, InquiryReplyRequestDTO request) {
        // 원본 문의 조회
        Inquires parentInquiry = inquiryRepository.findById(inquiryId)
                .orElseThrow(() -> new EntityNotFoundException("문의를 찾을 수 없습니다: " + inquiryId));

        // 답변 엔티티 생성 (관리자 정보는 임시로 null 처리)
        Inquires reply = Inquires.builder()
                .parent(parentInquiry)
                .users(parentInquiry.getUsers()) // 답변도 같은 사용자와 연결
                .admins(null) // 임시로 null (관리자 엔티티 연결 추후 구현)
                .title("Re: " + parentInquiry.getTitle()) // DB 관리용 제목 (프론트에서는 필요 없음)
                .content(request.content())
                .inquiryType(parentInquiry.getInquiryType())
                .inquiryReceiveMethod(parentInquiry.getInquiryReceiveMethod())
                .inquiryStatus(InquiryStatus.ANSWERED)
                .inquiryUrgentLevel(request.urgentLevel())
                .build();

        // 답변 저장
        Inquires savedReply = inquiryRepository.save(reply);

        // 원본 문의 상태를 ANSWERED로 변경
        parentInquiry.setInquiryStatus(InquiryStatus.ANSWERED);
        parentInquiry.setInquiryUrgentLevel(request.urgentLevel());
        inquiryRepository.save(parentInquiry);

        log.info("답변 등록 완료 - replyId: {}, parentInquiryId: {}, adminId: {}",
                savedReply.getId(), inquiryId, adminId);

        return InquiryResponseDTO.replied(savedReply);
    }


    // 문의 상세 응답 DTO 생성 (공통 로직)
    private InquiryDetailResponseDTO buildInquiryDetailResponse(Inquires inquiry) {

        // 포맷터와 시간대 선언 추가
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        ZoneId koreaZone = ZoneId.of("Asia/Seoul");

        // 주문 정보 DTO 생성
        InquiryDetailResponseDTO.OrderInfo orderInfo = null;
        if (inquiry.getOrders() != null) {
            Orders order = inquiry.getOrders();
            orderInfo = new InquiryDetailResponseDTO.OrderInfo(
                    order.getId(),
                    order.getOrderNumber().toString(),
                    order.getCreatedAt().withZoneSameInstant(koreaZone).format(formatter)
            );
        }

        // 답변 목록 DTO 생성 - 단일 답변으로 변경
        List<InquiryReplyResponseDTO> replies = new ArrayList<>();

        if (inquiry.getInquiryStatus() == InquiryStatus.ANSWERED) {
            Optional<Inquires> replyOptional = inquiryRepository.findByParent_Id(inquiry.getId());
            if (replyOptional.isPresent()) {
                replies.add(InquiryReplyResponseDTO.from(replyOptional.get()));
            }
        }

        List<InquiryAttachmentDTO> attachedImages = new ArrayList<>();
        return InquiryDetailResponseDTO.from(inquiry, replies, attachedImages);
    }


    // providerId로 Users 엔티티 조회
    // JWT의 providerId와 provider 정보로 사용자 검색
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
}