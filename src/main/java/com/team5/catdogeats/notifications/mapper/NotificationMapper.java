package com.team5.catdogeats.notifications.mapper;

import com.team5.catdogeats.notifications.domain.dto.NotificationResponseDTO;
import org.apache.ibatis.annotations.*;
import org.apache.ibatis.scripting.xmltags.XMLLanguageDriver;
import org.springframework.data.repository.query.Param;

import java.time.ZonedDateTime;
import java.util.List;

@Mapper
public interface NotificationMapper {

    @Select("""
        SELECT 
            nr.id as receiver_id,
            nr.notification_id,
            n.notification_type,
            n.title,
            n.message,
            nr.is_read,
            nr.read_at,
            nr.created_at
        FROM notification_receiver nr
        JOIN notifications n ON nr.notification_id = n.id
        JOIN users u ON nr.user_id = u.id
        <where>
            u.provider = #{provider}
            AND u.provider_id = #{providerId}
            
            <if test="cursorCreatedAt != null and cursorId != null">
                AND (nr.created_at &lt; #{cursorCreatedAt}
                     OR (nr.created_at = #{cursorCreatedAt} AND nr.id &lt; #{cursorId}))
            </if>
        </where>
        ORDER BY nr.created_at DESC, nr.id DESC
        LIMIT #{size}
        """)
    @Lang(XMLLanguageDriver.class)
    @Results(id = "NotificationResultMap", value = {
            @Result(column = "receiver_id", property = "id", id = true),
            @Result(column = "notification_id", property = "notificationId"),
            @Result(column = "notification_type", property = "notificationType"),
            @Result(column = "title", property = "title"),
            @Result(column = "message", property = "message"),
            @Result(column = "is_read", property = "isRead"),
            @Result(column = "read_at", property = "readAt"),
            @Result(column = "created_at", property = "createdAt")
    })
    List<NotificationResponseDTO> findNotificationsWithCursorAndReadFilter(
            @Param("provider") String provider,
            @Param("providerId") String providerId,
            @Param("cursorCreatedAt") ZonedDateTime cursorCreatedAt,
            @Param("cursorId") String cursorId,
            @Param("size") int size
    );
}
