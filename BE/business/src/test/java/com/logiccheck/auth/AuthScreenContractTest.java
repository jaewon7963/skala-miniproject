package com.logiccheck.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.business.BusinessApplication;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 화면이 실제로 보내고 읽는 모양 그대로 인증 경로를 확인한다.
 *
 * <p>필드 이름 하나만 어긋나도 로그인 직후 토큰이 비어 그다음 요청이 전부 401이 되는데,
 * 화면에는 "로그인이 필요합니다"만 뜨고 원인이 드러나지 않는다. 그래서 키 이름까지 못 박는다.
 */
@SpringBootTest(classes = BusinessApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthScreenContractTest {

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

    @Test
    void signupReturnsTokenUnderTheKeyTheScreenReads() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"signup@example.com","password":"logic1234",
                                 "agreeTerms":true,"agreePrivacy":true}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.email").value("signup@example.com"))
                .andExpect(jsonPath("$.user.organization").value("example.com"))
                .andExpect(jsonPath("$.user.id").isString());
    }

    @Test
    void rejectsSignupWithoutBothAgreements() throws Exception {
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"noagree@example.com","password":"logic1234",
                                 "agreeTerms":true,"agreePrivacy":false}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }

    @Test
    void rejectsDuplicateEmailWithTheMessageTheScreenShows() throws Exception {
        String body = """
                {"email":"dup@example.com","password":"logic1234","agreeTerms":true,"agreePrivacy":true}""";
        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").value("이미 가입된 이메일입니다."));
    }

    /** 화면은 "로그인 상태 유지" 체크값을 같이 보낸다. 서버가 안 쓰더라도 거절하면 안 된다. */
    @Test
    void acceptsLoginWithTheExtraFieldTheScreenSends() throws Exception {
        signUp("keep@example.com");

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"keep@example.com","password":"logic1234","keepSignedIn":true}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void changesPasswordAndRefusesTheOldOne() throws Exception {
        String token = signUp("pw@example.com");

        mockMvc.perform(patch("/api/auth/me/password").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"currentPassword":"logic1234","newPassword":"logic5678"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true));

        mockMvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"pw@example.com","password":"logic1234"}"""))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    /** 화면은 DELETE 요청에도 본문을 실어 비밀번호를 보낸다. */
    @Test
    void withdrawsWithPasswordInTheRequestBody() throws Exception {
        String token = signUp("bye@example.com");

        mockMvc.perform(delete("/api/auth/me").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"logic1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ok").value(true))
                .andExpect(jsonPath("$.purgeAfterDays").value(30));

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String signUp(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"logic1234\","
                                + "\"agreeTerms\":true,\"agreePrivacy\":true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("token").asText();
    }
}
