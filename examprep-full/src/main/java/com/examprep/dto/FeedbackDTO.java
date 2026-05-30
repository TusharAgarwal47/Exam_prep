package com.examprep.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class FeedbackDTO {
    private Long id;
    private String message;
    private String submittedBy;
    private String subjectCode;
    private Boolean isRead;
    private LocalDateTime submittedAt;
}
