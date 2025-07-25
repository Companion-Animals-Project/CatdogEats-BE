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
import com.team5.catdogeats.global.annotation.MongoTransactional;
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
            validateMessageContent(dto.message());
            String targetId = getTargetId(userId, chatRooms);
            log.debug("메시지 전송 준비: senderId={}, targetId={}, roomId={}",
                    userId, targetId, dto.roomId());

            saveMessage(dto, userId, sentAt);
            updateRoomInformation(dto, userId, sentAt);
            sendingSubscribe(dto, userId, sentAt, targetId);

            return dto;
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

    private void updateRoomInformation(ChatMessageDTO dto, String userId, Instant sentAt        ) {
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

    private void validateMessageContent(String message) {
        if (message == null || message.trim().isEmpty()) {
            throw new IllegalArgumentException("빈 메시지는 전송할 수 없습니다.");
        }

        if (message.length() > 1000) {
            throw new IllegalArgumentException("메시지는 1000자 이내로 작성해주세요.");
        }

        // XSS 방지 등 추가 검증 로직
        if (containsScript(message)) {
            throw new IllegalArgumentException("허용되지 않는 내용입니다.");
        }
    }

    private boolean containsScript(String message) {
        String lowerCase = message.toLowerCase();
        return lowerCase.contains("<script") ||
                lowerCase.contains("javascript:") ||
                lowerCase.contains("onload=") ||
                lowerCase.contains("onerror=");
    }
}
