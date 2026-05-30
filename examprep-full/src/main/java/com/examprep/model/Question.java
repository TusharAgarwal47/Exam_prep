package com.examprep.model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Question {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "question_number") private String questionNumber;
    private Integer marks;
    @Column(name = "course_outcome") private String courseOutcome;
    @Column(name = "question_type") private String questionType;
    @Column(columnDefinition = "TEXT") private String questionText;
    @Column(columnDefinition = "TEXT") private String normalizedText;
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paper_id")
    private QuestionPaper questionPaper;
}