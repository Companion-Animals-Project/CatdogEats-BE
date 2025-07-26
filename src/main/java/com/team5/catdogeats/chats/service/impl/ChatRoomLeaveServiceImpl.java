package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomDeleteRequestDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.ChatRoomLeaveService;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.chats.util.ChatRoomLockHelper;
import com.team5.catdogeats.global.annotation.MongoTransactional;
import com.team5.catdogeats.users.domain.enums.Role;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatRoomLeaveServiceImpl implements ChatRoomLeaveService {
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomLockHelper lockHelper;
    private final UserIdCacheService userIdCacheService;

    @MongoTransactional
    public void leaveRoom(ChatRoomDeleteRequestDTO dto, UserPrincipal userPrincipal) {
        lockHelper.executeWithLock(dto.roomId(), () -> {
            String userId = getUserId(userPrincipal);

            // 채팅방 존재 확인
            ChatRooms chatRoom = chatRoomRepository.findById(dto.roomId())
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + dto.roomId()));

            // 사용자 역할 조회
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);
            Instant leftAt = Instant.now();

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                handleBuyerLeave(chatRoom, dto.roomId(), userId, leftAt);
            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                handleSellerLeave(chatRoom, dto.roomId(), userId, leftAt);
            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }
        });
    }

    @Override
    @MongoTransactional
    public void rejoinRoom(String roomId, String userId) {
        lockHelper.executeWithLock(roomId, () -> {
            // 채팅방 존재 확인
            ChatRooms chatRoom = chatRoomRepository.findById(roomId)
                    .orElseThrow(() -> new NoSuchElementException("존재하지 않는 채팅방입니다: " + roomId));

            // 사용자 역할 조회
            String userRole = userIdCacheService.getCachedRoleByUserId(userId);
            Instant rejoinAt = Instant.now();

            if (Role.ROLE_BUYER.toString().equals(userRole)) {
                handleBuyerRejoin(chatRoom, roomId, userId, rejoinAt);
            } else if (Role.ROLE_SELLER.toString().equals(userRole)) {
                handleSellerRejoin(chatRoom, roomId, userId, rejoinAt);
            } else {
                throw new IllegalStateException("허용되지 않은 역할입니다: " + userRole);
            }
        });
    }

    private void handleBuyerLeave(ChatRooms chatRoom, String roomId, String userId, Instant leftAt) {
        if (!chatRoom.isBuyerActive()) {
            log.warn("구매자가 이미 나간 채팅방입니다: roomId={}, userId={}", roomId, userId);
            return;
        }

        // 구매자 나가기 상태 업데이트 (안읽은 메시지도 함께 초기화)
        chatRoomRepository.updateBuyerLeftStatus(roomId, leftAt, false);
        log.debug("구매자가 채팅방을 나갔습니다: roomId={}, userId={}", roomId, userId);

        // 나가기 메시지 추가
        String leaveMessage = "%s님이 채팅방을 나갔습니다.".formatted(chatRoom.getBuyerName());

        // 판매자가 활성 상태라면 안읽은 메시지 증가
        if (chatRoom.isSellerActive()) {
            chatRoomRepository.updateLastMessageAndIncrementSellerUnread(
                    roomId, leaveMessage, leftAt, userId, BehaviorType.LEAVE, 1);
        } else {
            chatRoomRepository.updateLastMessage(roomId, leaveMessage, leftAt, userId, BehaviorType.LEAVE);
        }
    }

    private void handleSellerLeave(ChatRooms chatRoom, String roomId, String userId, Instant leftAt) {
        if (!chatRoom.isSellerActive()) {
            log.warn("판매자가 이미 나간 채팅방입니다: roomId={}, userId={}", roomId, userId);
            return;
        }

        // 판매자 나가기 상태 업데이트 (안읽은 메시지도 함께 초기화)
        chatRoomRepository.updateSellerLeftStatus(roomId, leftAt, false);
        log.debug("판매자가 채팅방을 나갔습니다: roomId={}, userId={}", roomId, userId);

        // 나가기 메시지 추가
        String leaveMessage = "%s님이 채팅방을 나갔습니다.".formatted(chatRoom.getSellerName());

        // 구매자가 활성 상태라면 안읽은 메시지 증가
        if (chatRoom.isBuyerActive()) {
            chatRoomRepository.updateLastMessageAndIncrementBuyerUnread(
                    roomId, leaveMessage, leftAt, userId, BehaviorType.LEAVE, 1);
        } else {
            chatRoomRepository.updateLastMessage(roomId, leaveMessage, leftAt, userId, BehaviorType.LEAVE);
        }
    }

    private void handleBuyerRejoin(ChatRooms chatRoom, String roomId, String userId, Instant rejoinAt) {
        if (chatRoom.isBuyerActive()) {
            log.warn("구매자가 이미 참여중인 채팅방입니다: roomId={}, userId={}", roomId, userId);
            return;
        }

        // 재입장 시 leftAt 시간을 유지하면서 활성 상태만 변경
        chatRoomRepository.updateBuyerRejoinStatusKeepLeftAt(roomId, rejoinAt, true);
        log.debug("구매자가 채팅방에 다시 입장했습니다: roomId={}, userId={}", roomId, userId);

        // 재입장 메시지 추가
        String rejoinMessage = "%s님이 채팅방에 다시 입장했습니다.".formatted(chatRoom.getBuyerName());

        // 판매자가 활성 상태라면 안읽은 메시지 증가
        if (chatRoom.isSellerActive()) {
            chatRoomRepository.updateLastMessageAndIncrementSellerUnread(
                    roomId, rejoinMessage, rejoinAt, userId, BehaviorType.ENTER, 1);
        } else {
            chatRoomRepository.updateLastMessage(roomId, rejoinMessage, rejoinAt, userId, BehaviorType.ENTER);
        }
    }

    private void handleSellerRejoin(ChatRooms chatRoom, String roomId, String userId, Instant rejoinAt) {
        if (chatRoom.isSellerActive()) {
            log.warn("판매자가 이미 참여중인 채팅방입니다: roomId={}, userId={}", roomId, userId);
            return;
        }

        // 재입장 시 leftAt 시간을 유지하면서 활성 상태만 변경
        chatRoomRepository.updateSellerRejoinStatusKeepLeftAt(roomId, rejoinAt, true);
        log.debug("판매자가 채팅방에 다시 입장했습니다: roomId={}, userId={}", roomId, userId);

        // 재입장 메시지 추가
        String rejoinMessage = "%s님이 채팅방에 다시 입장했습니다.".formatted(chatRoom.getSellerName());

        // 구매자가 활성 상태라면 안읽은 메시지 증가
        if (chatRoom.isBuyerActive()) {
            chatRoomRepository.updateLastMessageAndIncrementBuyerUnread(
                    roomId, rejoinMessage, rejoinAt, userId, BehaviorType.ENTER, 1);
        } else {
            chatRoomRepository.updateLastMessage(roomId, rejoinMessage, rejoinAt, userId, BehaviorType.ENTER);
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
}