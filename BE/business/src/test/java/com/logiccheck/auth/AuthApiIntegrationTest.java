package com.logiccheck.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import com.example.business.BusinessApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.logiccheck.auth.entity.Session;
import com.logiccheck.auth.repository.SessionRepository;
import com.logiccheck.global.security.JwtTokenProvider;
import com.logiccheck.user.entity.User;
import com.logiccheck.user.repository.UserRepository;

@SpringBootTest(classes = BusinessApplication.class)
@AutoConfigureMockMvc
@Testcontainers
@Transactional
class AuthApiIntegrationTest {
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
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserRepository userRepository;

    @Autowired
    SessionRepository sessionRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Test
    void signsUpAndStoresOnlyPasswordAndRefreshTokenHashes() throws Exception {
        String response = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"member@example.com","password":"logic1234"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.id").isString())
                .andExpect(jsonPath("$.user.status").value("ACTIVE"))
                .andExpect(jsonPath("$.user.createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist())
                .andReturn().getResponse().getContentAsString();

        JsonNode body = objectMapper.readTree(response);
        User user = userRepository.findByEmail("member@example.com").orElseThrow();
        Session session = sessionRepository.findAll().get(0);
        assertThat(passwordEncoder.matches("logic1234", user.getPasswordHash())).isTrue();
        assertThat(session.getRefreshTokenHash()).isNotEqualTo(body.get("refreshToken").asText());
    }

    @Test
    void separatesRequestPasswordAndDuplicateErrors() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"bad\",\"password\":\"logic1234\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"weak@example.com\",\"password\":\"password\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD"));

        String request = "{\"email\":\"duplicate@example.com\",\"password\":\"logic1234\"}";
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/users").contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_EXISTS"));
    }

    @Test
    void logsInAndDoesNotRevealFailureReason() throws Exception {
        userRepository.save(User.create("login@example.com", passwordEncoder.encode("logic1234")));

        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"logic1234\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andExpect(jsonPath("$.user.email").value("login@example.com"));

        String unknownEmail = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unknown@example.com\",\"password\":\"logic1234\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();
        String wrongPassword = mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@example.com\",\"password\":\"wrong1234\"}"))
                .andExpect(status().isUnauthorized())
                .andReturn().getResponse().getContentAsString();

        assertThat(unknownEmail).isEqualTo(wrongPassword);
    }

    @Test
    void returnsCurrentUserAndRejectsMissingToken() throws Exception {
        User user = userRepository.save(User.create("me@example.com", passwordEncoder.encode("logic1234")));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(user.getId().toString()))
                .andExpect(jsonPath("$.email").value("me@example.com"))
                .andExpect(jsonPath("$.createdAt").value(org.hamcrest.Matchers.endsWith("+09:00")));

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void rotatesRefreshTokenAndRejectsThePreviousToken() throws Exception {
        String signup = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"refresh@example.com\",\"password\":\"logic1234\"}"))
                .andReturn().getResponse().getContentAsString();
        String oldRefreshToken = objectMapper.readTree(signup).get("refreshToken").asText();

        String refreshed = mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString())
                .andExpect(jsonPath("$.refreshToken").isString())
                .andReturn().getResponse().getContentAsString();

        assertThat(objectMapper.readTree(refreshed).get("refreshToken").asText()).isNotEqualTo(oldRefreshToken);
        assertThat(sessionRepository.findAll().get(0).getExpiresAt())
                .isBetween(java.time.Instant.now().plus(java.time.Duration.ofDays(13)),
                        java.time.Instant.now().plus(java.time.Duration.ofDays(15)));

        mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + oldRefreshToken + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logsOutOneSessionAndBlocksRefresh() throws Exception {
        String signup = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"logout@example.com\",\"password\":\"logic1234\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(signup);

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + tokens.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @Test
    void logoutWithoutBodyRevokesAllUserSessions() throws Exception {
        String signup = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"all@example.com\",\"password\":\"logic1234\"}"))
                .andReturn().getResponse().getContentAsString();
        JsonNode tokens = objectMapper.readTree(signup);
        mockMvc.perform(post("/api/users/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"all@example.com\",\"password\":\"logic1234\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + tokens.get("accessToken").asText()))
                .andExpect(status().isNoContent());

        assertThat(sessionRepository.findAll()).allMatch(session -> session.getRevokedAt() != null);
    }

    @Test
    void cannotRevokeAnotherUsersSession() throws Exception {
        JsonNode first = objectMapper.readTree(mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"first@example.com\",\"password\":\"logic1234\"}"))
                .andReturn().getResponse().getContentAsString());
        JsonNode second = objectMapper.readTree(mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"second@example.com\",\"password\":\"logic1234\"}"))
                .andReturn().getResponse().getContentAsString());

        mockMvc.perform(post("/api/users/logout")
                        .header("Authorization", "Bearer " + first.get("accessToken").asText())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + second.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/users/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"" + second.get("refreshToken").asText() + "\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void handlesCorsMissingRouteAndTamperedToken() throws Exception {
        User user = userRepository.save(User.create("security@example.com", passwordEncoder.encode("logic1234")));
        String accessToken = jwtTokenProvider.createAccessToken(user.getId());

        mockMvc.perform(options("/api/users/me")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"));

        mockMvc.perform(get("/api/missing")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken + "x"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }
}
