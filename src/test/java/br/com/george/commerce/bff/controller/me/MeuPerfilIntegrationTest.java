package br.com.george.commerce.bff.controller.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.OAuth2ClientRequestPostProcessor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /api/me/profile contra uma commerce-api falsa de verdade (servidor HTTP do próprio Java).
 * O token da sessão decide o que a API falsa responde.
 */
@SpringBootTest(properties = {"nani.auth.client-secret=segredo-de-teste", "nani.api.read-timeout=500ms"})
class MeuPerfilIntegrationTest {

    /** O que a API falsa recebeu por último. */
    record Received(String method, String path, String authorization, String requestId, String body) {
    }

    private static final AtomicReference<Received> LAST = new AtomicReference<>();
    private static final HttpServer API = startFakeApi();

    @DynamicPropertySource
    static void apiUrl(DynamicPropertyRegistry properties) {
        properties.add("nani.api.url", () -> "http://localhost:" + API.getAddress().getPort());
    }

    @AfterAll
    static void stopFakeApi() {
        API.stop(0);
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        LAST.set(null);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    /** Ana logada, com o token da sessão que o BFF deve mandar para a API. */
    private static MockHttpServletRequestBuilder asAna(MockHttpServletRequestBuilder request, String accessToken) {
        Instant now = Instant.now();
        OAuth2ClientRequestPostProcessor tokenNaSessao = oauth2Client("nani").accessToken(new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER, accessToken, now, now.plusSeconds(300)));
        return request
                .with(oidcLogin().idToken(token -> token
                        .subject("ana-sub")
                        .claim("name", "Ana Souza")
                        .claim("email", "ana@email.com")))
                .with(tokenNaSessao);
    }

    @Test
    void withoutSessionShouldAnswer401AndNotCallTheApi() throws Exception {
        mvc.perform(get("/api/me/profile")).andExpect(status().isUnauthorized());
        assertThat(LAST.get()).isNull();
    }

    @Test
    void shouldJoinKeycloakIdentityWithTheStoreProfile() throws Exception {
        mvc.perform(asAna(get("/api/me/profile"), "token-da-ana").header("X-Request-Id", "ABC12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.cpf").value("***.444.444-**"))
                .andExpect(jsonPath("$.phone").value("11987654321"));
        assertThat(LAST.get().method()).isEqualTo("GET");
        assertThat(LAST.get().path()).isEqualTo("/me");
        assertThat(LAST.get().authorization()).isEqualTo("Bearer token-da-ana");
        assertThat(LAST.get().requestId()).isEqualTo("ABC12345");
    }

    @Test
    void apiErrorShouldReachTheFrontAsItCame() throws Exception {
        mvc.perform(asAna(get("/api/me/profile"), "token-bloqueado"))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"code\":\"ACCOUNT_BLOCKED\",\"detail\":\"Conta bloqueada.\"}"));
    }

    @Test
    void slowApiShouldAnswer502WithAFixedMessage() throws Exception {
        mvc.perform(asAna(get("/api/me/profile"), "token-lento"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"))
                .andExpect(jsonPath("$.detail").value("Não conseguimos concluir agora. Tente de novo em alguns instantes."));
    }

    @Test
    void savingShouldSendPhoneAndCpfToTheApiAndReturnTheUpdatedProfile() throws Exception {
        mvc.perform(asAna(patch("/api/me/profile"), "token-da-ana").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"11987654321\",\"cpf\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.phone").value("11987654321"));
        assertThat(LAST.get().method()).isEqualTo("PATCH");
        assertThat(LAST.get().path()).isEqualTo("/me");
        assertThat(LAST.get().body()).contains("\"phone\":\"11987654321\"");
    }

    @Test
    void savingShouldRequireCsrf() throws Exception {
        mvc.perform(asAna(patch("/api/me/profile"), "token-da-ana")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"phone\":\"11987654321\"}"))
                .andExpect(status().isForbidden());
        assertThat(LAST.get()).isNull();
    }

    @Test
    void apiRuleErrorOnSaveShouldReachTheFrontAsItCame() throws Exception {
        mvc.perform(asAna(patch("/api/me/profile"), "token-cpf-em-uso").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"cpf\":\"39053344705\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CPF_IN_USE"));
    }

    private static HttpServer startFakeApi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", MeuPerfilIntegrationTest::answer);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        LAST.set(new Received(exchange.getRequestMethod(), exchange.getRequestURI().getPath(), authorization,
                exchange.getRequestHeaders().getFirst("X-Request-Id"), body));
        switch (authorization == null ? "" : authorization) {
            case "Bearer token-lento" -> {
                sleep();
                send(exchange, 200, "application/json", "{}");
            }
            case "Bearer token-cpf-em-uso" -> send(exchange, 409, "application/problem+json",
                    "{\"status\":409,\"code\":\"CPF_IN_USE\",\"detail\":\"Este CPF já está em outra conta.\"}");
            case "Bearer token-bloqueado" -> send(exchange, 403, "application/problem+json",
                    "{\"status\":403,\"code\":\"ACCOUNT_BLOCKED\",\"detail\":\"Conta bloqueada.\"}");
            default -> send(exchange, 200, "application/json", """
                    {"id":4,"name":"Ana Souza (nome antigo na loja)","cpf":"***.444.444-**",
                     "email":"ana@email.com","phone":"11987654321","active":true}""");
        }
    }

    private static void send(HttpExchange exchange, int status, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static void sleep() {
        try {
            Thread.sleep(1500);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
