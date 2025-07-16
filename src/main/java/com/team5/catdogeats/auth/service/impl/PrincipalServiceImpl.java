package com.team5.catdogeats.auth.service.impl;

import com.team5.catdogeats.auth.dto.AuthResponseDTO;
import com.team5.catdogeats.auth.dto.UserPrincipal;
import com.team5.catdogeats.auth.service.PrincipalService;
import com.team5.catdogeats.users.domain.Users;
import com.team5.catdogeats.users.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrincipalServiceImpl implements PrincipalService {
    private final UserRepository userRepository;

    @Override
    public AuthResponseDTO getPrincipal(UserPrincipal userPrincipal) {
        Users users = userRepository.findByProviderAndProviderId(userPrincipal.provider(), userPrincipal.providerId())
                .orElseThrow(() -> new NoSuchElementException("User not found for provider"));
        log.debug("name: {}, role: {}", users.getName(), users.getRole());
        return new AuthResponseDTO(users.getName(), users.getRole());
    }
}
