package com.team5.catdogeats.notifications.domain.dto;

import java.time.ZonedDateTime;

public record NotificationSearchRequestDTO(ZonedDateTime cursorCreatedAt,
                                            String cursorId){
}
