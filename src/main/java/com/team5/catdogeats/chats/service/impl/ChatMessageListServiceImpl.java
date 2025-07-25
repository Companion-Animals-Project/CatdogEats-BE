package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageResponseDTO;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatMessageListService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatMessageListServiceImpl implements ChatMessageListService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserIdCacheService userIdCacheService;
    private final MongoTemplate mongoTemplate;

    @Override
    public ChatMessagePageResponseDTO<ChatMessageListDTO> getMessagesWithCursor(String roomId,
                                                                                ChatMessagePageRequestDTO pageRequest,
                                                                                UserPrincipal userPrincipal) {
        String currentUserId = getUserId(userPrincipal);
        // 채팅방 정보 조회
        ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다."));

        validateUserParticipation(chatRoom, currentUserId);
        Instant accessLimit = getAccessLimit(chatRoom, currentUserId);

        Instant cursor = pageRequest.getCursorAsInstant();
        int size = pageRequest.size();
        Pageable pageable = PageRequest.of(0, size + 1, Sort.by(Sort.Direction.ASC, "sentAt"));

        List<ChatMessages> messages = chatMessageRepository.findMessagesWithDynamicQuery(mongoTemplate, roomId, accessLimit, cursor, pageable);

        boolean hasNext = messages.size() > size;

        if (hasNext) {
            messages = messages.subList(0, size);
        }

        List<ChatMessageListDTO> result = messages.stream()
                .map(msg -> ChatMessageListDTO.fromEntity(msg, currentUserId))
                .toList();


        String nextCursor = (hasNext && !messages.isEmpty())
                ? messages.get(messages.size() - 1).getSentAt().toString()
                : null;

        return ChatMessagePageResponseDTO.of(result, nextCursor, hasNext, size);

    }

    private String getUserId(UserPrincipal userPrincipal) {
        String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        if (userId == null) {
            userIdCacheService.cacheUserIdAndRole(userPrincipal.provider(), userPrincipal.providerId());
            userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        }
        return userId;
    }

    private void validateUserParticipation(ChatRooms chatRoom, String userId) {
        if (!chatRoom.getBuyerId().equals(userId) && !chatRoom.getSellerId().equals(userId)) {
            throw new IllegalStateException("해당 채팅방에 참여할 권한이 없습니다.");
        }
        log.debug("사용자 참여 검증 완료: roomId={}, userId={}, isActive={}",
                chatRoom.getId(), userId, chatRoom.isUserActive(userId));
    }

    private Instant getAccessLimit(ChatRooms chatRoom, String userId) {
        if (chatRoom.isUserActive(userId)) {
            return null; // 활성 사용자는 모든 메시지 조회 가능
        }

        Instant leftAt = chatRoom.getUserLeftAt(userId);
        log.debug("사용자 접근 제한 확인: userId={}, leftAt={}", userId, leftAt);

        return leftAt; // 나간 시간 이전 메시지만 조회 가능
    }
}

