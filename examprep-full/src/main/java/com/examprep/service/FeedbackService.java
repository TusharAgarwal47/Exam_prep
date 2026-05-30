package com.examprep.service;

import com.examprep.dto.FeedbackDTO;
import com.examprep.model.Feedback;
import com.examprep.repository.FeedbackRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    @Transactional
    public FeedbackDTO submitFeedback(String message, String submittedBy, String subjectCode) {
        if (message == null || message.trim().isEmpty())
            throw new IllegalArgumentException("Feedback message cannot be empty.");

        Feedback f = Feedback.builder()
                .message(message.trim())
                .submittedBy(submittedBy != null ? submittedBy.trim() : "Anonymous")
                .subjectCode(subjectCode)
                .isRead(false)
                .build();

        log.info("Feedback submitted by: {}", submittedBy);
        return toDTO(feedbackRepository.save(f));
    }

    public List<FeedbackDTO> getAllFeedback() {
        return feedbackRepository.findAllByOrderBySubmittedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public List<FeedbackDTO> getUnreadFeedback() {
        return feedbackRepository.findByIsReadFalseOrderBySubmittedAtDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public FeedbackDTO markAsRead(Long id) {
        Feedback f = feedbackRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Feedback not found: " + id));
        f.setIsRead(true);
        return toDTO(feedbackRepository.save(f));
    }

    public long getUnreadCount() {
        return feedbackRepository.countByIsReadFalse();
    }

    private FeedbackDTO toDTO(Feedback f) {
        return FeedbackDTO.builder()
                .id(f.getId()).message(f.getMessage())
                .submittedBy(f.getSubmittedBy()).subjectCode(f.getSubjectCode())
                .isRead(f.getIsRead()).submittedAt(f.getSubmittedAt()).build();
    }
}
