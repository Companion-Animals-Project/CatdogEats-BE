package com.team5.catdogeats.admins.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;

@RedisHash(value = "admin_session", timeToLive = 1800) // 30분
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminSession implements Serializable {

    @Id
    private String sessionId;

    @Indexed
    private String adminId;

    public boolean isValid() {
        return adminId != null && !adminId.trim().isEmpty();
    }
}
