package com.examprep.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "question_frequencies",
    uniqueConstraints = @UniqueConstraint(
        name = "uq_frequency_paper_text",
        columnNames = {"paper_code", "normalizedText"}
    )
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionFrequency {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "paper_code") private String paperCode;
    @Column(columnDefinition = "TEXT") private String normalizedText;
    private Integer frequency;
}
