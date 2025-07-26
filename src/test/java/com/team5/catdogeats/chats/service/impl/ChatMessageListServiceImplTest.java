package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatMessageListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatMessagePageResponseDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import com.team5.catdogeats.chats.mongo.repository.ChatMessageRepository;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatMessageListServiceImplTest {

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @Mock
    private ChatMessageRepository chatMessageRepository;

    @Mock
    private UserIdCacheService userIdCacheService;

    @InjectMocks
    private ChatMessageListServiceImpl chatMessageListService;

    private String roomId;
    private Users user1;
    private Users user2;
    private Sellers sellers;
    private Buyers buyers;
    private UserPrincipal principal;
    private Instant now;
    private ChatRooms room;

    @BeforeEach
    void setUp() {
        roomId = "room-123";

        user1 = Users.builder()
                .id("user-uuid-123")
                .provider("kakao")
                .providerId("12345678")
                .name("Test User")
                .build();
        user2 = Users.builder()
                .id("user-uuid-234")
                .provider("kakao")
                .providerId("87654321")
                .name("Other User")
                .build();
        sellers = Sellers.builder()
                .userId(user1.getId())
                .vendorName("Test Vendor")
                .vendorProfileImage("https://example.com/image.jpg")
                .businessNumber("123-45-67890")
                .build();
        buyers = Buyers.builder()
                .userId(user2.getId())
                .build();
        now = Instant.parse("2025-07-25T12:00:00Z");

        room = ChatRooms.builder()
                .buyerId(buyers.getUserId())
                .sellerId(sellers.getUserId())
                .buyerName(user2.getName())
                .sellerName(user1.getName())
                .createdAt(now)
                .updatedAt(now)
                .buyerLastReadAt(now)
                .sellerLastReadAt(now)
                .lastMessage("How are you?")
                .lastMessageAt(now)
                .lastSenderId(user2.getName())
                .lastBehaviorType(BehaviorType.TALK)
                .buyerUnreadCount(0)
                .sellerUnreadCount(1) // 판매자에게는 입장 알림이 안읽음으로 표시
                .buyerLastSeenAt(now)
                .sellerLastSeenAt(null) // 판매자는 아직 방에 들어오지 않음
                .buyerActive(true)     // @Builder.Default로 설정되지만 명시적으로 표시
                .sellerActive(true)
                .build();

        when(chatRoomRepository.findById(roomId))
                .thenReturn(Optional.of(room));
        when(userIdCacheService.getCachedUserId("kakao", "87654321"))
                .thenReturn(user2.getId());

        principal = new UserPrincipal("kakao", "87654321");
    }

    @Test
    void testGetMessagesWithCursor_shouldReturnPagedMessages() {
        // given
        Instant cursor = now.minusSeconds(60);
        ChatMessagePageRequestDTO pageRequest = new ChatMessagePageRequestDTO(cursor.toString(), 20);

        // prepare mock messages
        ChatMessages msg1 = ChatMessages.builder()
                .id("m1")
                .roomId(roomId)
                .senderId(buyers.getUserId())
                .behaviorType(BehaviorType.TALK)
                .message("Hello")
                .sentAt(now.minusSeconds(50))
                .readAt(now.minusSeconds(40))
                .build();
        ChatMessages msg2 = ChatMessages.builder()
                .id("m2")
                .roomId(roomId)
                .senderId(sellers.getUserId())
                .behaviorType(BehaviorType.TALK)
                .message("Hi there")
                .sentAt(now.minusSeconds(55))
                .readAt(now.minusSeconds(30))
                .build();
        ChatMessages msg3 = ChatMessages.builder()
                .id("m3")
                .roomId(roomId)
                .senderId(buyers.getUserId())
                .behaviorType(BehaviorType.TALK)
                .message("How are you?")
                .sentAt(now)
                .readAt(null)
                .build();
        List<ChatMessages> mockMessages = List.of(msg1, msg2, msg3);

        // stub repository
        when(chatMessageRepository.findMessagesWithDynamicQuery(
                any(), eq(roomId), isNull(), eq(cursor), any(Pageable.class)))
                .thenReturn(mockMessages);

        // when
        ChatMessagePageResponseDTO<ChatMessageListDTO> response =
                chatMessageListService.getMessagesWithCursor(roomId, pageRequest, principal);

        // then
        assertThat(response.contents()).hasSize(3);
        assertThat(response.hasNext()).isFalse();
        // verify mapping isMe
        assertThat(response.contents().get(0).isMe()).isTrue();
        assertThat(response.contents().get(1).isMe()).isFalse();
    }
}
