package com.team5.catdogeats.chats.service;

import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.chats.domain.dto.ChatRoomDeleteRequestDTO;

public interface ChatRoomLeaveService {
    void leaveRoom(ChatRoomDeleteRequestDTO dto, UserPrincipal userPrincipal);
    void rejoinRoom(String roomId, String userId);
}
