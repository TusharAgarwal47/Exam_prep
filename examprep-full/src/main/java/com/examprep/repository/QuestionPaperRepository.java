package com.examprep.repository;
import com.examprep.model.QuestionPaper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface QuestionPaperRepository extends JpaRepository<QuestionPaper, Long> {
    boolean existsByPaperCodeAndYear(String paperCode, Integer year);
    Optional<QuestionPaper> findByPaperCode(String paperCode);
}
