package com.examprep.controller;

import com.examprep.dto.ApiResponse;
import com.examprep.model.Question;
import com.examprep.model.QuestionFrequency;
import com.examprep.model.QuestionPaper;
import com.examprep.repository.QuestionFrequencyRepository;
import com.examprep.repository.QuestionPaperRepository;
import com.examprep.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class QuestionController {

    private final QuestionRepository questionRepository;
    private final QuestionPaperRepository questionPaperRepository;
    private final QuestionFrequencyRepository questionFrequencyRepository;

    /**
     * GET /api/questions/papers
     */
    @GetMapping("/papers")
    public ResponseEntity<ApiResponse<List<QuestionPaper>>> getAllPapers() {
        return ResponseEntity.ok(
            ApiResponse.ok("Fetched all papers.", questionPaperRepository.findAll()));
    }

    /**
     * POST /api/questions/papers
     * Create a new question paper (called from Upload Paper form).
     */
    @PostMapping("/papers")
    public ResponseEntity<ApiResponse<QuestionPaper>> createPaper(
            @RequestBody Map<String, Object> body) {
        try {
            QuestionPaper paper = new QuestionPaper();
            paper.setSubject((String) body.getOrDefault("subject", ""));
            paper.setPaperCode((String) body.getOrDefault("paperCode", ""));
            paper.setYear(Integer.parseInt(body.getOrDefault("year", 0).toString()));
            paper.setSemester((String) body.getOrDefault("semester", ""));
            paper.setExamType((String) body.getOrDefault("examType", ""));
            paper.setTotalMarks(Integer.parseInt(body.getOrDefault("totalMarks", 0).toString()));
            QuestionPaper saved = questionPaperRepository.save(paper);
            return ResponseEntity.ok(ApiResponse.ok("Paper created.", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/questions
     * Add a single question to an existing paper.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Question>> addQuestion(
            @RequestBody Map<String, Object> body) {
        try {
            String paperCode = (String) body.get("paperCode");
            QuestionPaper paper = questionPaperRepository.findByPaperCode(paperCode)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperCode));
            Question q = new Question();
            q.setQuestionText((String) body.getOrDefault("questionText", ""));
            q.setMarks(Integer.parseInt(body.getOrDefault("marks", 0).toString()));
            q.setQuestionPaper(paper);
            Question saved = questionRepository.save(q);
            return ResponseEntity.ok(ApiResponse.ok("Question added.", saved));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("Failed: " + e.getMessage()));
        }
    }

    /**
     * POST /api/questions/upload-pdf
     * Accepts a PDF file + paperCode, extracts text, auto-parses numbered
     * questions (lines starting with Q1/Q.1/1. etc.) and saves them.
     */
    @PostMapping("/upload-pdf")
    public ResponseEntity<ApiResponse<List<Question>>> uploadPdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("paperCode") String paperCode) {
        try {
            QuestionPaper paper = questionPaperRepository.findByPaperCode(paperCode)
                    .orElseThrow(() -> new RuntimeException("Paper not found: " + paperCode));

            // Extract raw text from PDF (PDFBox 3.x uses Loader.loadPDF)
            String text;
            try (PDDocument doc = Loader.loadPDF(file.getBytes())) {
                PDFTextStripper stripper = new PDFTextStripper();
                text = stripper.getText(doc);
            }

            // ── Smart parser for Graphic Era scanned exam PDFs ──
            List<Question> saved = new ArrayList<>();

            // Pre-process: normalize common OCR errors in the full text
            // "QI." → "Q1."  "Q2l." → "Q21." etc.  Also handle bold markers
            text = text.replaceAll("(?m)^QI\\.", "Q1.")   // OCR: I mistaken for 1
                       .replaceAll("(?m)^Ql\\.", "Q1.")
                       .replaceAll("(?i)\\(2Xl\\s*0=20", "(2X10=20")  // OCR: l→1, 0→0
                       .replaceAll("(?i)\\(2X\\s*l\\s*0=", "(2X10=")
                       .replaceAll("(?m)^\\*\\*([a-cA-C])\\.", "$1.")  // remove bold markers
                       .replaceAll("(?m)^([a-cA-C])\\s*\\.\\s+", "$1. "); // normalize "a ." → "a."

            String[] lines = text.split("\\r?\\n");

            // Noise patterns to skip entirely
            java.util.regex.Pattern noisePattern = java.util.regex.Pattern.compile(
                "(?i)(" +
                "page\\s+\\d+\\s+of|roll\\s*no\\s*\\.{3}|time\\s*:\\s*\\d|maximum\\s*marks|" +
                "graphic\\s*era|hill\\s*uni|gehu\\/|semester\\s*exam|paper\\s*code|" +
                "name\\s*of\\s*the\\s*(paper|course)|established|adhintyan|sankhya|ugc\\s*act|" +
                "b\\.?tech|integrated|gehu|uttarakhand|legislature|" +
                "sample\\s*input:|sample\\s*output:|^output:\\s|" +
                "^input\\s*the\\s*detail|^number\\s*of\\s*student|^provide\\s*name|" +
                "^total\\s*no\\.|^roll\\s*no\\.?=|^name\\s*=|^department\\s*=|" +
                "^university\\s*name\\s*=|^name[-–]|^roll\\s*no\\.?[-–]|" +
                "area\\s*of\\s*rect|area\\s*of\\s*tri|volume\\s*of\\s*(cone|cyl|hemi)|" +
                "^data\\s*members?:\\s*$|^member\\s*function:\\s*$|^string\\s*str\\s*$|" +
                "^note\\s*:?\\s*$|^\\*+\\s*$" +
                ")"
            );

            // Marks pattern: (2X10=20 Marks) or just "10 marks"
            java.util.regex.Pattern marksPattern = java.util.regex.Pattern
                    .compile("(?i)\\(\\d+\\s*[xX]\\s*\\d+\\s*=\\s*(\\d+)\\s*[Mm]arks\\)|(\\d+)\\s*[Mm]arks");

            // Main Q header: Q1. Q2. Q3. Q4. Q5. alone or with marks
            // Also handles "Q1" on its own line, "(Q1.)" etc.
            java.util.regex.Pattern mainQPattern = java.util.regex.Pattern
                    .compile("(?i)^\\(?Q\\s*([1-9])\\s*[.):]?\\s*(\\(.*\\))?\\s*$");

            // Sub-question: "a." "b." "c." at line start — relaxed: just needs letter + . + space
            // Also handles bold: "a. text" "b. text" even with minimal content after
            java.util.regex.Pattern subQPattern = java.util.regex.Pattern
                    .compile("^([a-cA-C])[.):]\\s+(.{3,})");

            // Note-item lines to skip: (i) (ii) (iii) (iv)
            java.util.regex.Pattern noteItemPattern = java.util.regex.Pattern
                    .compile("^\\(([ivxIVX]+|\\d)\\)\\s");

            java.util.LinkedHashMap<String, StringBuilder> qMap = new java.util.LinkedHashMap<>();
            java.util.LinkedHashMap<String, Integer> marksMap = new java.util.LinkedHashMap<>();
            String currentKey = "";
            String currentMainQNum = "";
            int currentMarks = 10;

            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;

                // Skip note items like (i) (ii) (iii) (iv)
                if (noteItemPattern.matcher(trimmed).find()) continue;
                // Skip noise
                if (noisePattern.matcher(trimmed).find()) continue;
                // Skip very short lines
                if (trimmed.length() < 3) continue;

                // Extract marks from this line
                java.util.regex.Matcher mm = marksPattern.matcher(trimmed);
                if (mm.find()) {
                    try {
                        String mg = mm.group(1) != null ? mm.group(1) : mm.group(2);
                        if (mg != null) {
                            int total = Integer.parseInt(mg);
                            currentMarks = total > 15 ? total / 2 : total;
                        }
                    } catch (Exception ignored) {}
                }

                // Skip pure marks lines like "(2X10=20 Marks)" or "(2Xl 0=20 Marks)"
                if (trimmed.matches("(?i)^\\(2X.*[Mm]arks\\).*") && trimmed.length() < 30) continue;
                // Skip "(Q3." style heading artifacts
                if (trimmed.matches("(?i)^\\(Q\\d+\\..*")) {
                    // still detect Q number
                    java.util.regex.Matcher qm2 = java.util.regex.Pattern
                            .compile("(?i)\\(Q\\s*([1-9])").matcher(trimmed);
                    if (qm2.find()) { currentMainQNum = "Q" + qm2.group(1); currentKey = ""; }
                    continue;
                }

                // Detect main Q header
                java.util.regex.Matcher mainM = mainQPattern.matcher(trimmed);
                if (mainM.find()) {
                    currentMainQNum = "Q" + mainM.group(1);
                    currentKey = "";
                    continue;
                }

                // Detect sub-question a. b. c.
                java.util.regex.Matcher subM = subQPattern.matcher(trimmed);
                if (subM.find()) {
                    String subLetter = subM.group(1).toLowerCase();
                    String newKey = currentMainQNum + subLetter;
                    // Only start new sub-q if we have a main Q context OR it's clearly a new one
                    if (!currentMainQNum.isEmpty() || !qMap.containsKey(newKey)) {
                        currentKey = newKey;
                        if (!qMap.containsKey(currentKey)) {
                            qMap.put(currentKey, new StringBuilder());
                            marksMap.put(currentKey, currentMarks);
                        }
                        // Append — some PDFs repeat the sub-letter on continuation lines
                        qMap.get(currentKey).append(trimmed);
                        continue;
                    }
                }

                // Continuation of current sub-question
                if (!currentKey.isEmpty() && qMap.containsKey(currentKey)) {
                    // Skip pure CO-reference lines like "CO2" "C03" "COS"
                    if (trimmed.matches("(?i)^C[Oo0][1-9]\\s*$")) continue;
                    // Skip bullet points that are part of sample output/input
                    if (trimmed.matches("^[-•*]\\s+.*") && trimmed.length() < 40) continue;
                    qMap.get(currentKey).append(" ").append(trimmed);
                }
            }

            // Save all collected questions in Q1a, Q1b... Q5c order
            for (Map.Entry<String, StringBuilder> entry : qMap.entrySet()) {
                String qText = entry.getValue().toString().trim();
                // Clean trailing CO references: "CO1" "(CO2)" "(CO1,CO4)"
                qText = qText.replaceAll("\\s*\\(?CO[\\d,\\s]+\\)?\\s*$", "").trim();
                // Clean trailing marks references
                qText = qText.replaceAll("\\s*\\(\\d+X\\d+=\\d+\\s*Marks\\)\\s*$", "").trim();
                // Clean repeated sub-letter at start if duplicated
                qText = qText.replaceAll("^([a-cA-C][.):])\\s+\\1\\s+", "$1 ").trim();

                if (qText.length() < 10) continue;

                Question q = new Question();
                q.setQuestionText(qText);
                q.setMarks(marksMap.getOrDefault(entry.getKey(), 10));
                q.setQuestionPaper(paper);
                saved.add(questionRepository.save(q));
            }

            if (saved.isEmpty())
                return ResponseEntity.ok(ApiResponse.ok(
                    "PDF parsed but no numbered questions found. Try adding questions manually.", saved));

            return ResponseEntity.ok(ApiResponse.ok(
                "Extracted and saved " + saved.size() + " question(s) from PDF.", saved));

        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("PDF processing failed: " + e.getMessage()));
        }
    }

    /**
     * GET /api/questions/by-paper/{paperCode}
     */
    @GetMapping("/by-paper/{paperCode}")
    public ResponseEntity<ApiResponse<List<Question>>> getByPaper(
            @PathVariable String paperCode) {
        List<Question> questions = questionRepository.findByQuestionPaper_PaperCode(paperCode);
        if (questions.isEmpty())
            return ResponseEntity.status(404).body(ApiResponse.error("No questions found for paper: " + paperCode));
        return ResponseEntity.ok(ApiResponse.ok("Fetched questions.", questions));
    }

    /**
     * GET /api/questions/frequency/{paperCode}
     */
    @GetMapping("/frequency/{paperCode}")
    public ResponseEntity<ApiResponse<List<QuestionFrequency>>> getFrequency(
            @PathVariable String paperCode) {
        List<QuestionFrequency> freqs = questionFrequencyRepository
                .findByPaperCodeOrderByFrequencyDesc(paperCode);
        return ResponseEntity.ok(ApiResponse.ok("Fetched frequencies.", freqs));
    }

    /**
     * GET /api/questions/frequent/by-subject/{subject}
     * Returns questions ranked by how many times they appear across all
     * papers of the given subject — most repeated first.
     */
    @GetMapping("/frequent/by-subject/{subject}")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFrequentBySubject(
            @PathVariable String subject) {
        // Find all papers for this subject
        List<QuestionPaper> papers = questionPaperRepository.findAll().stream()
                .filter(p -> subject.equalsIgnoreCase(p.getSubject()))
                .toList();
        if (papers.isEmpty())
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("No papers found for subject: " + subject));

        // Aggregate question text → total frequency across all papers
        java.util.Map<String, Integer> freqMap = new java.util.LinkedHashMap<>();
        for (QuestionPaper paper : papers) {
            List<QuestionFrequency> freqs = questionFrequencyRepository
                    .findByPaperCodeOrderByFrequencyDesc(paper.getPaperCode());
            for (QuestionFrequency f : freqs) {
                freqMap.merge(f.getNormalizedText(), f.getFrequency(), Integer::sum);
            }
        }

        // Sort by frequency descending and build response list
        List<Map<String, Object>> result = freqMap.entrySet().stream()
                .sorted(java.util.Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(e -> {
                    Map<String, Object> row = new java.util.LinkedHashMap<>();
                    row.put("questionText", e.getKey());
                    row.put("frequency", e.getValue());
                    return row;
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(
                "Top questions for subject: " + subject, result));
    }

    /**
     * GET /api/questions/subjects
     * Returns a distinct list of all subject names across all papers.
     */
    @GetMapping("/subjects")
    public ResponseEntity<ApiResponse<List<String>>> getAllSubjects() {
        List<String> subjects = questionPaperRepository.findAll().stream()
                .map(QuestionPaper::getSubject)
                .filter(s -> s != null && !s.isBlank())
                .distinct()
                .sorted()
                .toList();
        return ResponseEntity.ok(ApiResponse.ok("Fetched subjects.", subjects));
    }
}
