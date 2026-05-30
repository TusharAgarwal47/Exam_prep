package com.examprep.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginResponseDTO {
    private Long id;
    private String username;
    private String displayName;
    private String role;          // "ADMIN" or "STUDENT"
    private boolean success;
    private String message;
}
