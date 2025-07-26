package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomDeleteRequestDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.chats.util.ChatRoomLockHelper;
import com.team5.catdogeats.users.domain.enums.Role;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatRoomLeaveServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatRoomLockHelper lockHelper;

    @Mock
    private UserIdCacheService userIdCacheService;

    @InjectMocks
    private ChatRoomLeaveServiceImpl chatRoomLeaveService;

    private final String roomId = "room-123";
    private final String userId = "user-abc";

    private final UserPrincipal userPrincipal = new UserPrincipal("kakao", "123456");

    @Test
    void leaveRoom_shouldUpdateBuyerLeaveStatusAndSendLeaveMessage() {
        // given
        ChatRooms mockRoom = Mockito.mock(ChatRooms.class);
        when(userIdCacheService.getCachedUserId("kakao", "123456")).thenReturn(userId);
        when(userIdCacheService.getCachedRoleByUserId(userId)).thenReturn(Role.ROLE_BUYER.toString());
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));

        when(mockRoom.isBuyerActive()).thenReturn(true);
        when(mockRoom.isSellerActive()).thenReturn(true);
        when(mockRoom.getBuyerName()).thenReturn("홍길동");

        ChatRoomDeleteRequestDTO dto = new ChatRoomDeleteRequestDTO(roomId);

        // when
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();  // simulate lock execution
            return null;
        }).when(lockHelper).executeWithLock(eq(roomId), any(Runnable.class));

        chatRoomLeaveService.leaveRoom(dto, userPrincipal);

        // then
        verify(chatRoomRepository).updateBuyerLeftStatus(eq(roomId), any(), eq(false));
        verify(chatRoomRepository).updateLastMessageAndIncrementSellerUnread(
                eq(roomId),
                contains("홍길동님이 채팅방을 나갔습니다."),
                any(),
                eq(userId),
                eq(BehaviorType.LEAVE),
                eq(1)
        );
    }

    @Test
    void rejoinRoom_shouldUpdateBuyerRejoinStatusAndSendRejoinMessage() {
        // given
        ChatRooms mockRoom = Mockito.mock(ChatRooms.class);
        when(chatRoomRepository.findById(roomId)).thenReturn(Optional.of(mockRoom));
        when(userIdCacheService.getCachedRoleByUserId(userId)).thenReturn(Role.ROLE_BUYER.toString());

        when(mockRoom.isBuyerActive()).thenReturn(false);
        when(mockRoom.isSellerActive()).thenReturn(true);
        when(mockRoom.getBuyerName()).thenReturn("김철수");

        // when
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(1);
            task.run();  // simulate lock execution
            return null;
        }).when(lockHelper).executeWithLock(eq(roomId), any(Runnable.class));

        chatRoomLeaveService.rejoinRoom(roomId, userId);

        // then
        verify(chatRoomRepository).updateBuyerRejoinStatusKeepLeftAt(eq(roomId), any(), eq(true));
        verify(chatRoomRepository).updateLastMessageAndIncrementSellerUnread(
                eq(roomId),
                contains("김철수님이 채팅방에 다시 입장했습니다."),
                any(),
                eq(userId),
                eq(BehaviorType.ENTER),
                eq(1)
        );
    }
}
