package com.team5.catdogeats.chats.service.impl;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.dto.ChatRoomListDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageRequestDTO;
import com.team5.catdogeats.chats.domain.dto.ChatRoomPageResponseDTO;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import com.team5.catdogeats.chats.mongo.repository.ChatRoomRepository;
import com.team5.catdogeats.chats.service.UserIdCacheService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.domain.enums.Role;
import com.team5.catdogeats.users.domain.mapping.Buyers;
import com.team5.catdogeats.users.domain.mapping.Sellers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatRoomListServiceImplTest {

    @Mock
    private UserIdCacheService userIdCacheService;

    @Mock
    private ChatRoomRepository chatRoomRepository;

    @InjectMocks
    private ChatRoomListServiceImpl chatRoomListService;

    private Users user1;
    private Users user2;
    private Users user3;
    private Users user4;
    private Sellers sellers1;
    private Sellers sellers2;
    private Sellers sellers3;
    private Buyers buyers;
    private UserPrincipal principal;
    private Instant now;
    private ChatRooms room1;
    private ChatRooms room2;
    private ChatRooms room3;
    private ChatRoomPageRequestDTO pageRequest;

    @BeforeEach
    void setUp() {
        user1 = Users.builder()
                .id("user-uuid-123")
                .role(Role.ROLE_SELLER)
                .provider("kakao")
                .providerId("12345678")
                .name("Test User")
                .build();
        user2 = Users.builder()
                .id("user-uuid-234")
                .role(Role.ROLE_BUYER)
                .provider("kakao")
                .providerId("87654321")
                .name("Other User")
                .build();

        user3 = Users.builder()
                .id("user-uuid-345")
                .role(Role.ROLE_SELLER)
                .provider("naver")
                .providerId("12345678")
                .name("Test User")
                .build();

        user4 = Users.builder()
                .role(Role.ROLE_SELLER)
                .provider("naver")
                .providerId("87654321")
                .name("Other User")
                .build();

        sellers1 = Sellers.builder()
                .userId(user1.getId())
                .vendorName("Test Vendor")
                .vendorProfileImage("https://example.com/image.jpg")
                .businessNumber("123-45-67890")
                .build();

        sellers2 = Sellers.builder()
                .userId(user3.getId())
                .vendorName("Test Vendor")
                .vendorProfileImage("https://example.com/image.jpg")
                .businessNumber("123-45-67890")
                .build();
        sellers3 = Sellers.builder()
                .userId(user4.getId())
                .vendorName("Test Vendor")
                .vendorProfileImage("https://example.com/image.jpg")
                .build();

        buyers = Buyers.builder()
                .userId(user2.getId())
                .build();
        now = Instant.parse("2025-07-25T12:00:00Z");

        room1 = ChatRooms.builder()
                .buyerId(buyers.getUserId())
                .sellerId(sellers1.getUserId())
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

        principal = new UserPrincipal("kakao", "87654321");
        when(userIdCacheService.getCachedUserId("kakao", "87654321"))
                .thenReturn(user2.getId());

        room2 = ChatRooms.builder()
                .buyerId(buyers.getUserId())
                .sellerId(sellers2.getUserId())
                .buyerName(user2.getName())
                .sellerName(user3.getName())
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


        room3 = ChatRooms.builder()
                .buyerId(buyers.getUserId())
                .sellerId(sellers3.getUserId())
                .buyerName(user2.getName())
                .sellerName(user4.getName())
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


    }

    @Test
    void getChatRooms_shouldReturnCursorPagedResult_forBuyer() {

        pageRequest = ChatRoomPageRequestDTO.builder()
                .cursor(null)
                .size(20)
                .build();

        List<ChatRooms> mockRooms = List.of(room1, room2, room3); // size = 3 > page size = 2

        // when
        when(userIdCacheService.getCachedUserId(principal.provider(), principal.providerId())).thenReturn(buyers.getUserId());
        when(userIdCacheService.getCachedRoleByUserId(buyers.getUserId())).thenReturn(Role.ROLE_BUYER.toString());
        when(chatRoomRepository.findByBuyerIdAndBuyerActiveTrueOrderByLastMessageAtDesc(eq(buyers.getUserId()), any()))
                .thenReturn(mockRooms);

        ChatRoomPageResponseDTO<ChatRoomListDTO> response =
                chatRoomListService.getChatRooms(principal, pageRequest);

        // then
        assertThat(response.content()).hasSize(3);
        assertThat(response.hasNext()).isFalse();
        assertThat(response.nextCursor()).isEqualTo(null);
    }

    @Test
    void getChatRooms_shouldReturnCursorPagedResult_forBuyer_hasNext() {
        pageRequest = ChatRoomPageRequestDTO.builder()
                .cursor(null)
                .size(2)
                .build();

        List<ChatRooms> mockRooms = List.of(room1, room2, room3); // size = 3 > page size = 2

        // when
        when(userIdCacheService.getCachedUserId(principal.provider(), principal.providerId())).thenReturn(buyers.getUserId());
        when(userIdCacheService.getCachedRoleByUserId(buyers.getUserId())).thenReturn(Role.ROLE_BUYER.toString());
        when(chatRoomRepository.findByBuyerIdAndBuyerActiveTrueOrderByLastMessageAtDesc(eq(buyers.getUserId()), any()))
                .thenReturn(mockRooms);

        ChatRoomPageResponseDTO<ChatRoomListDTO> response =
                chatRoomListService.getChatRooms(principal, pageRequest);

        // then
        assertThat(response.content()).hasSize(2);
        assertThat(response.hasNext()).isTrue();
        assertThat(response.nextCursor()).isEqualTo(room3.getLastMessageAt().toString());
    }
}
