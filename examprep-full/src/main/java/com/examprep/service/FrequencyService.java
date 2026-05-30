package com.examprep.service;

import com.examprep.model.Question;
import com.examprep.model.QuestionFrequency;
import com.examprep.repository.QuestionFrequencyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FrequencyService {

    private final QuestionFrequencyRepository frequencyRepository;

    /**
     * For each question, increments the frequency counter for that
     * (paperCode, normalizedText) pair, or creates a new record at 1.
     *
     * NOTE: The `year` parameter was removed — year is already captured
     * on the QuestionPaper entity itself and is not needed here.
     */
    @Transactional
    public void updateFrequencies(List<Question> questions, String paperCode) {
        for (Question q : questions) {
            String key = q.getNormalizedText();
            if (key == null || key.isBlank()) continue;
            frequencyRepository.findByPaperCodeAndNormalizedText(paperCode, key)
                    .ifPresentOrElse(
                        freq -> {
                            freq.setFrequency(freq.getFrequency() + 1);
                            frequencyRepository.save(freq);
                        },
                        () -> frequencyRepository.save(QuestionFrequency.builder()
                                .paperCode(paperCode).normalizedText(key).frequency(1).build())
                    );
        }
        log.info("Updated frequencies for {} questions in paper {}", questions.size(), paperCode);
    }
}
