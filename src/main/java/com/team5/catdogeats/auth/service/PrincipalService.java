package com.team5.catdogeats.auth.service;

import com.team5.catdogeats.auth.dto.AuthResponseDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;

public interface PrincipalService {
    AuthResponseDTO getPrincipal(UserPrincipal userPrincipal);
}
