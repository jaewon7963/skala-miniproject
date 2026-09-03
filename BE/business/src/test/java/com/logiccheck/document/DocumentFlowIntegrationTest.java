package com.logiccheck.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.ByteArrayOutputStream;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
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
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.business.BusinessApplication;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiccheck.global.security.JwtTokenProvider;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.repository.UserRepository;

@SpringBootTest(classes = BusinessApplication.class)
@AutoConfigureMockMvc
@Testcontainers
class DocumentFlowIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("jwt.secret", () -> "test-secret-key-with-at-least-32-bytes");
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

    private String bearer(String email) {
        User user = userRepository.save(User.create(email, passwordEncoder.encode("logic1234")));
        return "Bearer " + jwtTokenProvider.createAccessToken(user.getId());
    }

    @Test
    void listsSevenGlobalTags() throws Exception {
        mockMvc.perform(get("/api/tags").header("Authorization", bearer("tags@example.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(7));
    }

    @Test
    void rejectsRequestsWithoutToken() throws Exception {
        mockMvc.perform(get("/api/documents")).andExpect(status().isUnauthorized());
    }

    @Test
    void uploadParsePollListDetailSectionsPagesUpdateDeleteFlow() throws Exception {
        String auth = bearer("flow@example.com");
        byte[] pdf = onePagePdf();

        String uploadJson = mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "test.pdf", "application/pdf", pdf))
                        .header("Authorization", auth))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.displayStatus").value("PARSE_PENDING"))
                .andExpect(jsonPath("$.pageCount").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        String documentId = objectMapper.readTree(uploadJson).get("id").asText();

        waitUntilParsed(documentId, auth);

        mockMvc.perform(get("/api/documents").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='" + documentId + "')].displayStatus").value("IDLE"));

        mockMvc.perform(get("/api/documents/" + documentId).header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mimeType").value("application/pdf"));

        mockMvc.perform(get("/api/documents/" + documentId + "/sections").header("Authorization", auth))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/documents/" + documentId + "/pages").header("Authorization", auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pageNo").value(1));

        mockMvc.perform(patch("/api/documents/" + documentId)
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"수정된 제목\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("수정된 제목"));

        mockMvc.perform(delete("/api/documents/" + documentId).header("Authorization", auth))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/documents/" + documentId).header("Authorization", auth))
                .andExpect(status().isNotFound());
    }

    @Test
    void uploadValidationRejectsBadFilesAndDuplicates() throws Exception {
        String auth = bearer("dup@example.com");
        byte[] notPdf = "이것은 PDF가 아닙니다".getBytes();
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "fake.pdf", "application/pdf", notPdf))
                        .header("Authorization", auth))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_FILE_TYPE"));

        byte[] pdf = onePagePdf();
        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "dup.pdf", "application/pdf", pdf))
                        .header("Authorization", auth))
                .andExpect(status().isCreated());

        mockMvc.perform(multipart("/api/documents")
                        .file(new MockMultipartFile("file", "dup2.pdf", "application/pdf", pdf))
                        .header("Authorization", auth))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_FILE"));
    }

    private void waitUntilParsed(String documentId, String auth) throws Exception {
        for (int i = 0; i < 25; i++) {
            MockHttpServletRequestBuilder req = get("/api/documents/" + documentId + "/parse-status")
                    .header("Authorization", auth);
            String body = mockMvc.perform(req).andReturn().getResponse().getContentAsString();
            String parseStatus = objectMapper.readTree(body).get("parseStatus").asText();
            if ("DONE".equals(parseStatus) || "FAILED".equals(parseStatus)) {
                assertThat(parseStatus).isEqualTo("DONE");
                return;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("문서 파싱이 시간 내에 끝나지 않았습니다: " + documentId);
    }

    private byte[] onePagePdf() throws Exception {
        try (PDDocument document = new PDDocument()) {
            document.addPage(new PDPage());
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }
}
