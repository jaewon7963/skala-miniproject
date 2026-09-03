package com.logiccheck.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.business.BusinessApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiccheck.global.security.JwtTokenProvider;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.repository.UserRepository;

/**
 * 업로드부터 검토 완료까지를 화면이 부르는 순서 그대로 태워본다.
 *
 * <p>비동기 파이프라인이 끼어 있어 트랜잭션으로 묶지 않는다. 묶으면 백그라운드 스레드가
 * 커밋되지 않은 데이터를 못 봐서 영원히 기다리게 된다.
 */
@SpringBootTest(classes = BusinessApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class ReviewJobFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "test-secret-key-with-at-least-32-bytes");
        // 흐름만 확인하면 되므로 진행 구간을 짧게 줄인다.
        registry.add("review.parse-duration-ms", () -> "300");
        registry.add("review.analyze-duration-ms", () -> "300");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    void uploadAnalyzeReviewCompleteFlow() throws Exception {
        String auth = bearer("review-flow@example.com");
        String documentId = upload(auth);
        String jobId = startAnalysis(auth, documentId);

        JsonNode done = pollUntilTerminal(auth, jobId);
        assertThat(done.get("status").asText()).isEqualTo("DONE");
        assertThat(done.get("parseProgress").asInt()).isEqualTo(100);
        assertThat(done.get("analyzeProgress").asInt()).isEqualTo(100);
        // 진행 화면이 단계 상태를 그대로 소문자로 바꿔 쓰므로 비어 있으면 안 된다.
        done.get("steps").forEach(step -> assertThat(step.get("state").asText()).isEqualTo("DONE"));

        // 분석이 끝나면 문서는 검토 대기 상태로 보인다.
        mockMvc.perform(get("/api/documents/" + documentId).header("Authorization", auth))
                .andExpect(jsonPath("$.status").value("REVIEWING"))
                .andExpect(jsonPath("$.latestJobId").value(jobId));

        mockMvc.perform(get("/api/review-jobs/" + jobId + "/sections").header("Authorization", auth))
                .andExpect(status().isOk());

        JsonNode pages = readJson(mockMvc.perform(get("/api/review-jobs/" + jobId + "/pages")
                .header("Authorization", auth)).andExpect(status().isOk()));
        assertThat(pages.get(0).get("blocks")).isNotEmpty();

        JsonNode findings = readJson(mockMvc.perform(get("/api/review-jobs/" + jobId + "/findings")
                .header("Authorization", auth)).andExpect(status().isOk()));
        assertThat(findings).isNotEmpty();
        assertEveryFindingAnchorsIntoTheDocument(findings, pages);

        String findingId = findings.get(0).get("id").asText();
        decide(auth, jobId, findingId, "ACCEPTED");
        // 미판정으로 되돌리면 판정 시각도 같이 지워진다.
        JsonNode reverted = decide(auth, jobId, findingId, "PENDING");
        assertThat(reverted.hasNonNull("decidedAt")).isFalse();
        decide(auth, jobId, findingId, "ACCEPTED");

        mockMvc.perform(post("/api/review-jobs/" + jobId + "/complete").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completedAt").isNotEmpty())
                .andExpect(jsonPath("$.summary.accepted").value(1));

        mockMvc.perform(post("/api/review-jobs/" + jobId + "/complete").header("Authorization", auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ALREADY_COMPLETED"));

        mockMvc.perform(get("/api/documents/" + documentId).header("Authorization", auth))
                .andExpect(jsonPath("$.status").value("DONE"));

        // 의견서 화면이 방어 없이 꺼내 쓰는 세 유형 키는 항상 있어야 한다.
        mockMvc.perform(get("/api/review-jobs/" + jobId + "/report").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewer").value("review-flow@example.com"))
                .andExpect(jsonPath("$.summary.byType.ERROR").exists())
                .andExpect(jsonPath("$.summary.byType.NEEDS_CHECK").exists())
                .andExpect(jsonPath("$.summary.byType.NO_EVIDENCE").exists())
                .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test
    void rejectsSecondAnalysisWhileOneIsRunning() throws Exception {
        String auth = bearer("duplicate-job@example.com");
        String documentId = upload(auth);
        startAnalysis(auth, documentId);

        mockMvc.perform(post("/api/review-jobs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + documentId + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("JOB_ALREADY_RUNNING"));
    }

    @Test
    void reportsNoJobForUnanalyzedDocument() throws Exception {
        String auth = bearer("no-job@example.com");
        String documentId = upload(auth);

        mockMvc.perform(get("/api/documents/" + documentId + "/review-jobs/latest").header("Authorization", auth))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NO_REVIEW_JOB"));
    }

    @Test
    void hidesOtherPeoplesJobs() throws Exception {
        String owner = bearer("owner@example.com");
        String stranger = bearer("stranger@example.com");
        String jobId = startAnalysis(owner, upload(owner));

        mockMvc.perform(get("/api/review-jobs/" + jobId).header("Authorization", stranger))
                .andExpect(status().isForbidden());
    }

    /* ------------------------------------------------------------------ */

    /** 검토 항목의 근거는 반드시 원문에 실제로 있는 블록을 가리켜야 한다. */
    private void assertEveryFindingAnchorsIntoTheDocument(JsonNode findings, JsonNode pages) {
        java.util.Set<String> blockIds = new java.util.HashSet<>();
        pages.forEach(page -> page.get("blocks").forEach(block -> blockIds.add(block.get("id").asText())));

        findings.forEach(finding -> {
            assertThat(finding.get("evidence")).as("근거 없는 검토 항목은 저장되지 않는다").isNotEmpty();
            finding.get("evidence").forEach(evidence ->
                    assertThat(blockIds).contains(evidence.get("anchorId").asText()));
        });
    }

    private JsonNode decide(String auth, String jobId, String findingId, String verdict) throws Exception {
        return readJson(mockMvc.perform(patch("/api/review-jobs/" + jobId + "/findings/" + findingId + "/verdict")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"verdict\":\"" + verdict + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verdict").value(verdict)));
    }

    private String upload(String auth) throws Exception {
        String body = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "plan.pdf", "application/pdf", businessPlanPdf()))
                        .header("Authorization", auth))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String startAnalysis(String auth, String documentId) throws Exception {
        String body = mockMvc.perform(post("/api/review-jobs").header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"documentId\":\"" + documentId + "\"}"))
                .andExpect(status().isAccepted())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private JsonNode pollUntilTerminal(String auth, String jobId) throws Exception {
        for (int i = 0; i < 100; i++) {
            JsonNode job = readJson(mockMvc.perform(get("/api/review-jobs/" + jobId).header("Authorization", auth))
                    .andExpect(status().isOk()));
            String status = job.get("status").asText();
            if ("DONE".equals(status) || "FAILED".equals(status)) {
                return job;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("분석이 시간 내에 끝나지 않았습니다: " + jobId);
    }

    private JsonNode readJson(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return objectMapper.readTree(actions.andReturn().getResponse().getContentAsString());
    }

    private String bearer(String email) {
        User user = userRepository.save(User.create(email, passwordEncoder.encode("logic1234")));
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId());
    }

    /** 표 합계가 어긋난 문서. 규칙 기반 분석이 최소 한 건은 잡아내야 한다. */
    private byte[] businessPlanPdf() throws Exception {
        String[] lines = {
                "1. Business overview",
                "This plan covers a guide robot for unmanned stores.",
                "2. Revenue plan",
                "Revenue of 2.4 billion is expected in 2027.",
                "[Table 5-1] Revenue estimate",
                "Item      2025      2026      2027      Total",
                "Revenue   320       960       2400      3680",
                "Cost      210       580       1340      2130",
                "Total     530       1540      3740      5000",
        };
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.COURIER), 11);
                content.setLeading(16);
                content.newLineAtOffset(50, 720);
                for (String line : lines) {
                    content.showText(line);
                    content.newLine();
                }
                content.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
