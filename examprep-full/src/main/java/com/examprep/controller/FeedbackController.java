package com.examprep.controller;

import com.examprep.dto.ApiResponse;
import com.examprep.dto.FeedbackDTO;
import com.examprep.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * POST /api/feedback/submit
     * Student submits general feedback.
     * Body: { "message": "Great app!", "submittedBy": "Rahul", "subjectCode": "TEV211" }
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<FeedbackDTO>> submit(
            @RequestBody Map<String, String> body) {
        try {
            FeedbackDTO result = feedbackService.submitFeedback(
                    body.get("message"),
                    body.getOrDefault("submittedBy", "Anonymous"),
                    body.getOrDefault("subjectCode", null));
            return ResponseEntity.ok(ApiResponse.ok("Feedback submitted successfully.", result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * GET /api/feedback/all
     * Admin views all feedback, newest first.
     */
    @GetMapping("/all")
    public ResponseEntity<ApiResponse<List<FeedbackDTO>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched all feedback.", feedbackService.getAllFeedback()));
    }

    /**
     * GET /api/feedback/unread
     * Admin views only unread feedback.
     */
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<FeedbackDTO>>> getUnread() {
        return ResponseEntity.ok(ApiResponse.ok("Fetched unread feedback.", feedbackService.getUnreadFeedback()));
    }

    /**
     * GET /api/feedback/unread-count
     * Returns count of unread feedbacks — used for admin dashboard notification badge.
     */
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        return ResponseEntity.ok(ApiResponse.ok("Unread count fetched.", feedbackService.getUnreadCount()));
    }

    /**
     * PUT /api/feedback/{id}/read
     * Admin marks a specific feedback as read.
     */
    @PutMapping("/{id}/read")
    public ResponseEntity<ApiResponse<FeedbackDTO>> markAsRead(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ApiResponse.ok("Marked as read.", feedbackService.markAsRead(id)));
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(ApiResponse.error(e.getMessage()));
        }
    }
}
