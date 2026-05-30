package com.examprep.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LoginRequestDTO {
    private String username;
    private String password;
}
