package com.team5.catdogeats.chats.mongo.repository;

import com.team5.catdogeats.chats.domain.mapping.ChatMessages;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.Instant;
import java.util.List;

public interface ChatMessageRepositoryCustom {

    default List<ChatMessages> findMessagesWithDynamicQuery(MongoTemplate mongoTemplate,
                                                            String roomId,
                                                            Instant leftAt,
                                                            Instant cursor,
                                                            Pageable pageable) {
        Query query = new Query();
        Criteria criteria = Criteria.where("roomId").is(roomId);

        if (leftAt != null) {
            criteria.and("sentAt").gt(leftAt);
        }

        if (cursor != null) {
            criteria.and("sentAt").lt(cursor);
        }

        query.addCriteria(criteria);
        query.with(pageable);

        return mongoTemplate.find(query, ChatMessages.class);
    }

}
