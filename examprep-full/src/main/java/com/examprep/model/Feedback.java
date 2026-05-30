package com.examprep.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "feedbacks")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "submitted_by")
    private String submittedBy;

    @Column(name = "subject_code")
    private String subjectCode;

    @Column(name = "is_read")
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @PrePersist
    protected void onCreate() {
        submittedAt = LocalDateTime.now();
    }
}
