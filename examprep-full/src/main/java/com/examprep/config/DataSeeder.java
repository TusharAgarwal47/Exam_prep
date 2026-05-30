package com.examprep.config;

import com.examprep.model.*;
import com.examprep.repository.*;
import com.examprep.service.FrequencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class DataSeeder {

    private final QuestionPaperRepository paperRepository;
    private final QuestionRepository questionRepository;
    private final QuestionFrequencyRepository frequencyRepository;
    private final FrequencyService frequencyService;
    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    @Profile("!test")
    public CommandLineRunner seedData() {
        return args -> {

            if (!userRepository.existsByUsername("admin")) {
                userRepository.save(User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin123"))
                        .role(User.Role.ADMIN)
                        .displayName("Administrator")
                        .build());
                log.info("Seeded admin user. username=admin password=admin123");
            }

            if (!userRepository.existsByUsername("student")) {
                userRepository.save(User.builder()
                        .username("student")
                        .password(passwordEncoder.encode("student123"))
                        .role(User.Role.STUDENT)
                        .displayName("Student")
                        .build());
                log.info("Seeded student user. username=student password=student123");
            }

            if (paperRepository.existsByPaperCodeAndYear("TEV211", 2025)) {
                log.info("Paper seed data already present. Skipping.");
                return;
            }

            log.info("Seeding TEV-211 March 2025 paper...");

            QuestionPaper paper = QuestionPaper.builder()
                    .subject("Environmental Science")
                    .paperCode("TEV211")
                    .year(2025)
                    .semester("II")
                    .examType("Term Evaluation (Even) Semester Examination March 2025")
                    .totalMarks(50)
                    .durationMinutes(90)
                    .originalFilename("TEV-211-MID-2025.pdf")
                    .filePath("uploads/papers/2025/TEV211_2025_seeded.pdf")
                    .build();
            paper = paperRepository.save(paper);

            List<Question> questions = List.of(
                Question.builder().questionNumber("Q1A").marks(10).courseOutcome("CO1").questionType("a")
                    .questionText("Explain the multidisciplinary nature of environmental science. Why study of environment is so important.")
                    .normalizedText("explain the multidisciplinary nature of environmental science why study of environment is so important")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q1B").marks(10).courseOutcome("CO1").questionType("b")
                    .questionText("Define the ecosystem. Discuss the components of an ecosystem with reference to biotic and abiotic.")
                    .normalizedText("define the ecosystem discuss the components of an ecosystem with reference to biotic and abiotic")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q2A").marks(10).courseOutcome("CO1").questionType("a")
                    .questionText("Define the terms Ecological pyramids, Habitat, Biosphere, Atmosphere and Ecology.")
                    .normalizedText("define the terms ecological pyramids habitat biosphere atmosphere and ecology")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q2B").marks(10).courseOutcome("CO1").questionType("b")
                    .questionText("Discuss the role of decomposers to maintain the ecosystem.")
                    .normalizedText("discuss the role of decomposers to maintain the ecosystem")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q3A").marks(10).courseOutcome("CO1").questionType("a")
                    .questionText("Discuss the demerits of hydropower dams in hill area.")
                    .normalizedText("discuss the demerits of hydropower dams in hill area")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q3B").marks(10).courseOutcome("CO1").questionType("b")
                    .questionText("Write the significance of forest as a resource.")
                    .normalizedText("write the significance of forest as a resource")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q4A").marks(10).courseOutcome("CO2").questionType("a")
                    .questionText("Define the soil, soil erosion and soil conservation methods.")
                    .normalizedText("define the soil soil erosion and soil conservation methods")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q4B").marks(10).courseOutcome("CO2").questionType("b")
                    .questionText("What are producers, consumers, and decomposers? Give examples or flow chart.")
                    .normalizedText("what are producers consumers and decomposers give examples or flow chart")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q5A").marks(10).courseOutcome("CO2").questionType("a")
                    .questionText("Compare the advantages and limitations of renewable energy sources such as solar, and wind.")
                    .normalizedText("compare the advantages and limitations of renewable energy sources such as solar and wind")
                    .questionPaper(paper).build(),
                Question.builder().questionNumber("Q5B").marks(10).courseOutcome("CO2").questionType("b")
                    .questionText("Explain in-situ and ex-situ conservation methods of biodiversity.")
                    .normalizedText("explain in situ and ex situ conservation methods of biodiversity")
                    .questionPaper(paper).build()
            );

            questionRepository.saveAll(questions);
            frequencyService.updateFrequencies(questions, "TEV211");
            log.info("Seeded {} questions for TEV211 2025.", questions.size());
        };
    }
}
