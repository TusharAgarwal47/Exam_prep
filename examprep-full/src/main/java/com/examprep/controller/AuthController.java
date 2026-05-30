package com.examprep.controller;

import com.examprep.dto.ApiResponse;
import com.examprep.dto.LoginRequestDTO;
import com.examprep.dto.LoginResponseDTO;
import com.examprep.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/login
     * Body: { "username": "admin", "password": "admin123" }
     * Returns role so Swing app knows which dashboard to open.
     *
     * Admin:   username=admin,   password=admin123
     * Student: username=student, password=student123
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO request) {

        LoginResponseDTO response = authService.login(request);

        if (response.isSuccess())
            return ResponseEntity.ok(ApiResponse.ok("Login successful.", response));

        return ResponseEntity.status(401).body(ApiResponse.error(response.getMessage()));
    }
}
