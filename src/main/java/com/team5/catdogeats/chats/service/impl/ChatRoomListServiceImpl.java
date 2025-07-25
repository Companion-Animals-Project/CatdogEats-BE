package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageResponseDTO;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomListService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.global.annotation.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomListServiceImpl implements ChatRoomListService {
    private final UserIdCacheService userIdCacheService;
    private final ChatRoomRepository chatRoomRepository;

    @Override
    @MongoTransactional(readOnly = true)
    public ChatRoomPageResponseDTO<ChatRoomListDTO> getChatRooms(UserPrincipal userPrincipal,
                                                                 ChatRoomPageRequestDTO pageRequest) {
        try {
            // 사용자 역할 조회
            String userId = getUserId(userPrincipal);
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);
            log.debug("userid {}, userrole {}", userId, userRole);

            Pageable pageable = PageRequest.of(0, pageRequest.size() + 1);
            Instant cursor = pageRequest.getCursorAsInstant();

            List<ChatRooms> chatRooms = getActiveChatRoomsWithCursor(userRole, userId, cursor, pageable);

            boolean hasNext = chatRooms.size() > pageRequest.size();
            if (hasNext) {
                chatRooms = chatRooms.subList(0, pageRequest.size());
            }

            // DTO 변환
            List<ChatRoomListDTO> chatRoomDTOs = chatRooms.stream()
                    .map(room -> ChatRoomListDTO.convertToChatRoomListDTO(room, userId, userRole))
                    .toList();


            String nextCursor = (hasNext && !chatRooms.isEmpty())
                    ? (chatRooms.get(chatRooms.size() - 1).getLastMessageAt() != null
                        ? chatRooms.get(chatRooms.size() - 1).getLastMessageAt().toString()
                        : null)
                    : null;

            return ChatRoomPageResponseDTO.of(
                    chatRoomDTOs,
                    nextCursor,
                    hasNext,
                    pageRequest.size());


        } catch (Exception e) {
            log.error("전체 채팅방 목록 조회 실패", e);
            throw e;
        }
    }

    private String getUserId(UserPrincipal userPrincipal) {
        String userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        if (userId == null) {
            userIdCacheService.cacheUserIdAndRole(userPrincipal.provider(), userPrincipal.providerId());
            userId = userIdCacheService.getCachedUserId(userPrincipal.provider(), userPrincipal.providerId());
        }
        return userId;
    }

    private List<ChatRooms> getActiveChatRoomsWithCursor(String userRole,
                                                         String userId,
                                                         Instant cursor,
                                                         Pageable pageable) {

        if (Role.ROLE_BUYER.toString().equals(userRole)) {
            return cursor != null
                    ? chatRoomRepository.findByBuyerIdAndBuyerActiveTrueAndLastMessageAtLessThanOrderByLastMessageAtDesc(userId, cursor, pageable)
                    : chatRoomRepository.findByBuyerIdAndBuyerActiveTrueOrderByLastMessageAtDesc(userId, pageable);

        } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
            return cursor != null
                    ? chatRoomRepository.findBySellerIdAndSellerActiveTrueAndLastMessageAtLessThanOrderByLastMessageAtDesc(userId, cursor, pageable)
                    : chatRoomRepository.findBySellerIdAndSellerActiveTrueOrderByLastMessageAtDesc(userId, pageable);

        } else {
            throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
        }
    }

}
