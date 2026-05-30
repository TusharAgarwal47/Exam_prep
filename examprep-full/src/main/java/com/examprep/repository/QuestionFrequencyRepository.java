package com.examprep.repository;

import com.examprep.model.QuestionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionFrequencyRepository extends JpaRepository<QuestionFrequency, Long> {
    Optional<QuestionFrequency> findByPaperCodeAndNormalizedText(String paperCode, String normalizedText);
    List<QuestionFrequency> findByPaperCodeOrderByFrequencyDesc(String paperCode);
}
