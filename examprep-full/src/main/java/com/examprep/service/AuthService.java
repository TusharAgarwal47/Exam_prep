package com.examprep.service;

import com.examprep.dto.LoginRequestDTO;
import com.examprep.dto.LoginResponseDTO;
import com.examprep.repository.UserRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public LoginResponseDTO login(LoginRequestDTO request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            return LoginResponseDTO.builder()
                    .success(false).message("Username and password are required.").build();
        }

        return userRepository.findByUsername(request.getUsername().trim())
                .map(user -> {
                    if (passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                        log.info("Login successful: {}", user.getUsername());
                        return LoginResponseDTO.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .displayName(user.getDisplayName())
                                .role(user.getRole().name())
                                .success(true)
                                .message("Login successful.")
                                .build();
                    }
                    return LoginResponseDTO.builder()
                            .success(false).message("Incorrect password.").build();
                })
                .orElse(LoginResponseDTO.builder()
                        .success(false).message("User not found.").build());
    }
}
