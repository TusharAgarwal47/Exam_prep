package com.examprep.repository;

import com.examprep.model.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {
    List<Feedback> findAllByOrderBySubmittedAtDesc();
    List<Feedback> findByIsReadFalseOrderBySubmittedAtDesc();
    long countByIsReadFalse();
}
