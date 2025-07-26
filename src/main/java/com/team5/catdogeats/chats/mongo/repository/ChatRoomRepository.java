package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.ChatRooms;
import com.team5.catdogeats.chats.domain.enums.BehaviorType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.Update;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRooms, String> {
    Optional<ChatRooms> findByBuyerIdAndSellerId(String buyerId, String sellerId);

    // 커서 페이징 용
    @Query(
            value = "{ 'buyerId': ?0, 'lastMessageAt': { $lt: ?1 } }",
            sort  = "{ 'lastMessageAt': -1 }"
    )
    List<ChatRooms> findByBuyerIdAndLastMessageAtLessThanOrderByLastMessageAtDesc(
            String buyerId,
            Instant cursor,
            Pageable pageable);

    // 활성 구매자 채팅방 커서 페이징
    @Query(
            value = "{ 'buyerId': ?0, 'buyerActive': true, 'lastMessageAt': { $lt: ?1 } }",
            sort  = "{ 'lastMessageAt': -1 }"
    )
    List<ChatRooms> findByBuyerIdAndBuyerActiveTrueAndLastMessageAtLessThanOrderByLastMessageAtDesc(
            String buyerId, Instant cursor, Pageable pageable);

    @Query(
            value = "{ 'buyerId': ?0, 'buyerActive': true }",
            sort  = "{ 'lastMessageAt': -1 }"
    )
    List<ChatRooms> findByBuyerIdAndBuyerActiveTrueOrderByLastMessageAtDesc(
            String buyerId, Pageable pageable);

    // 활성 판매자 채팅방 커서 페이징
    @Query(
            value = "{ 'sellerId': ?0, 'sellerActive': true, 'lastMessageAt': { $lt: ?1 } }",
            sort  = "{ 'lastMessageAt': -1 }"
    )
    List<ChatRooms> findBySellerIdAndSellerActiveTrueAndLastMessageAtLessThanOrderByLastMessageAtDesc(
            String sellerId, Instant cursor, Pageable pageable);

    @Query(
            value = "{ 'sellerId': ?0, 'sellerActive': true }",
            sort  = "{ 'lastMessageAt': -1 }"
    )
    List<ChatRooms> findBySellerIdAndSellerActiveTrueOrderByLastMessageAtDesc(
            String sellerId, Pageable pageable);


    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 } }")
    void updateLastMessage(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerUnreadCount': 0, 'buyerLastReadAt': ?1 } }")
    void resetBuyerUnreadCountAndUpdateLastReadAt(String roomId, Instant readAt);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerUnreadCount': 0, 'sellerLastReadAt': ?1 } }")
    void resetSellerUnreadCountAndUpdateLastReadAt(String roomId, Instant readAt);


    // 새 메시지 전송 시 한 번에 업데이트 (마지막 메시지 + 안읽은 개수 증가)
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 }, '$inc': { 'buyerUnreadCount': ?5 } }")
    void updateLastMessageAndIncrementBuyerUnread(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType, int increment);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'lastMessage': ?1, 'lastMessageAt': ?2, 'lastSenderId': ?3, 'lastBehaviorType': ?4, 'updatedAt': ?2 }, '$inc': { 'sellerUnreadCount': ?5 } }")
    void updateLastMessageAndIncrementSellerUnread(String roomId, String message, Instant sentAt, String senderId, BehaviorType behaviorType, int increment);

    // 구매자 나가기 상태 업데이트
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerLeftAt': ?1, 'buyerActive': ?2, 'updatedAt': ?1 } }")
    void updateBuyerLeftStatus(String roomId, Instant leftAt, boolean active);

    // 판매자 나가기 상태 업데이트
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerLeftAt': ?1, 'sellerActive': ?2, 'updatedAt': ?1 } }")
    void updateSellerLeftStatus(String roomId, Instant leftAt, boolean active);


    // 구매자 재입장 시 leftAt 시간 유지 (새로 추가)
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerActive': ?2, 'buyerLastSeenAt': ?1, 'updatedAt': ?1 } }")
    void updateBuyerRejoinStatusKeepLeftAt(String roomId, Instant rejoinAt, boolean active);

    // 판매자 재입장 시 leftAt 시간 유지 (새로 추가)
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerActive': ?2, 'sellerLastSeenAt': ?1, 'updatedAt': ?1 } }")
    void updateSellerRejoinStatusKeepLeftAt(String roomId, Instant rejoinAt, boolean active);

    // 비활성 사용자 활성화 메서드 추가
    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'buyerActive': true, 'buyerLastSeenAt': ?1, 'updatedAt': ?1 } }")
    void activateBuyer(String roomId, Instant activateAt);

    @Query("{ '_id': ?0 }")
    @Update("{ '$set': { 'sellerActive': true, 'sellerLastSeenAt': ?1, 'updatedAt': ?1 } }")
    void activateSeller(String roomId, Instant activateAt);
}