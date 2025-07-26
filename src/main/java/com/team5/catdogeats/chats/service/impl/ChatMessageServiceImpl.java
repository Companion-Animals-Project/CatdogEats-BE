package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageDTO;
import com.team5.catdogeats.chats.domain.dto.PublishDTO;
import com.team5.catdogeats.chats.domain.dto.SelfDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatMessageService;
import com.team5.catdogeats.chats.service.ChatRoomUpdateService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.annotation.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageServiceImpl implements ChatMessageService {
    private final ChatMessageRepository chatMessageRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomUpdateService chatRoomUpdateService;
    private final UserIdCacheService userIdCacheService;

    @Override
    @MongoTransactional
    public ChatMessageDTO saveAndPublish(ChatMessageDTO dto, String userId) {
        try {
            // 1) 전송 시간
            Instant sentAt = Instant.now();

            // 2) 채팅방 존재 확인
            ChatRooms chatRooms = chatRoomRepository.findById(dto.roomId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 방입니다."));

            // 사용자가 채팅방에 참여중인지 확인 (Soft Delete 체크)
            validateUserParticipation(chatRooms, userId);
            String targetId = getTargetId(userId, chatRooms);

            // 비활성 상대방이 있다면 활성화 처리
            activateInactiveTarget(chatRooms, userId, targetId, sentAt);

            log.debug("메시지 전송 준비: senderId={}, targetId={}, roomId={}",
                    userId, targetId, dto.roomId());

            saveMessage(dto, userId, sentAt);
            updateRoomInformation(dto, userId, sentAt);
            sendingSubscribe(dto, userId, sentAt, targetId);

            return dto;
        } catch (IllegalStateException e) {
            throw e;

        } catch (Exception e) {
            log.error("메시지 저장 및 전송 실패", e);
            throw e;
        }
    }

    private void validateUserParticipation(ChatRooms chatRoom, String userId) {
        if (!chatRoom.isUserActive(userId)) {
            throw new IllegalStateException("나간 채팅방에는 메시지를 보낼 수 없습니다.");
        }
    }

    private void activateInactiveTarget(ChatRooms chatRoom, String senderId, String targetId, Instant activateAt) {
        String senderRole = userIdCacheService.getCachedRoleByUserId(senderId);

        if (Role.ROLE_BUYER.toString().equals(senderRole)) {
            // 구매자가 보낸 메시지 → 판매자가 비활성 상태라면 활성화
            if (!chatRoom.isSellerActive()) {
                chatRoomRepository.activateSeller(chatRoom.getId(), activateAt);
                log.debug("비활성 판매자 활성화: roomId={}, sellerId={}", chatRoom.getId(), targetId);
            }
        } else if (Role.ROLE_SELLER.toString().equals(senderRole)) {
            // 판매자가 보낸 메시지 → 구매자가 비활성 상태라면 활성화
            if (!chatRoom.isBuyerActive()) {
                chatRoomRepository.activateBuyer(chatRoom.getId(), activateAt);
                log.debug("비활성 구매자 활성화: roomId={}, buyerId={}", chatRoom.getId(), targetId);
            }
        }
    }

    private void sendingSubscribe(ChatMessageDTO dto, String userId, Instant sentAt, String targetId) {
        // 7) 발신자에게 SelfDTO 전송
        SelfDTO self = SelfDTO.builder()
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .isMe(true)
                .build();
        redisTemplate.convertAndSend("user:" + userId, self);
        log.debug("Redis 발신자 채널 전송: user:{} -> {}", userId, self);

        // 8) 수신자에게 PublishDTO 전송
        PublishDTO publish = PublishDTO.builder()
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .isMe(false)
                .build();
        redisTemplate.convertAndSend("user:" + targetId, publish);
        log.debug("Redis 수신자 채널 전송: user:{} -> {}", targetId, publish);
    }

    private void updateRoomInformation(ChatMessageDTO dto, String userId, Instant sentAt) {
        // 7) 채팅방 정보 업데이트 (마지막 메시지 + 수신자 안읽은 개수 증가)
        chatRoomUpdateService.updateRoomOnNewMessage(
                dto.roomId(), userId, dto.message(), dto.behaviorType(), sentAt);
    }

    private void saveMessage(ChatMessageDTO dto, String userId, Instant sentAt) {
        // 6) MongoDB에 메시지 저장
        String messageId = UUID.randomUUID().toString();
        ChatMessages messages = ChatMessages.builder()
                .id(messageId)
                .roomId(dto.roomId())
                .senderId(userId)
                .message(dto.message())
                .behaviorType(dto.behaviorType())
                .sentAt(sentAt)
                .build();
        chatMessageRepository.save(messages);
        log.debug("메시지 저장 완료: id={}", messageId);
    }

    private String getTargetId(String userId, ChatRooms chatRooms) {
        String targetId = chatRooms.getOtherUserId(userId);
        if (targetId == null) {
            throw new IllegalStateException("채팅방 참여자가 아닙니다.");
        }
        return targetId;
    }
}