package com.examprep.model;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "question_papers")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class QuestionPaper {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String subject;
    @Column(name = "paper_code") private String paperCode;
    @Column(name = "exam_year") private Integer year;
    private String semester;
    @Column(name = "exam_type") private String examType;
    @Column(name = "total_marks") private Integer totalMarks;
    @Column(name = "duration_minutes") private Integer durationMinutes;
    @Column(name = "original_filename") private String originalFilename;
    @Column(name = "file_path") private String filePath;
}