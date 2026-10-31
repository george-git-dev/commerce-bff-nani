package br.com.george.commerce.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.servlet.http.Cookie;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Repasse /api/** → commerce-api, com uma API falsa de verdade (servidor HTTP do próprio Java). */
@SpringBootTest(properties = {"nani.auth.client-secret=segredo-de-teste", "nani.api.read-timeout=500ms"})
class CommerceApiProxyIntegrationTest {

    /** O que a API falsa recebeu por último. */
    record Received(String method, String uri, String authorization, String requestId, String cookie, String body) {
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

    @Test
    void visitorShouldReachThePublicCatalogWithoutToken() throws Exception {
        // Na URL (como o navegador manda): o .param() do MockMvc não preenche a query string.
        mvc.perform(get("/api/products?marca=lattafa"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/json"))
                .andExpect(jsonPath("$.ok").value(true));
        assertThat(LAST.get().method()).isEqualTo("GET");
        assertThat(LAST.get().uri()).isEqualTo("/products?marca=lattafa");
        assertThat(LAST.get().authorization()).isNull();
    }

    @Test
    void loggedCustomerShouldGoWithTheSessionTokenOnly() throws Exception {
        Instant now = Instant.now();
        mvc.perform(get("/api/me")
                        .with(oidcLogin())
                        // Token guardado na sessão para o cliente "nani" (o que o BFF repassa).
                        .with(oauth2Client("nani").accessToken(new OAuth2AccessToken(
                                OAuth2AccessToken.TokenType.BEARER, "token-da-ana", now, now.plusSeconds(300))))
                        .header("Authorization", "Bearer token-falso-do-navegador")
                        .header("X-Request-Id", "pedido-123")
                        .cookie(new Cookie("NANI_SESSION", "nao-vai-para-a-api")))
                .andExpect(status().isOk());
        assertThat(LAST.get().authorization()).isEqualTo("Bearer token-da-ana");
        assertThat(LAST.get().requestId()).isEqualTo("pedido-123");
        assertThat(LAST.get().cookie()).isNull();
    }

    @Test
    void writingShouldRequireCsrf() throws Exception {
        mvc.perform(post("/api/cart/items").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertThat(LAST.get()).isNull();
    }

    @Test
    void writingShouldForwardMethodBodyStatusAndLocation() throws Exception {
        mvc.perform(post("/api/cart/items").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"variantId\":7,\"quantity\":2}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/cart/items/1"));
        assertThat(LAST.get().method()).isEqualTo("POST");
        assertThat(LAST.get().body()).isEqualTo("{\"variantId\":7,\"quantity\":2}");
    }

    @Test
    void apiErrorsShouldReachTheFrontAsTheyCame() throws Exception {
        mvc.perform(get("/api/erro"))
                .andExpect(status().is(422))
                .andExpect(content().json("{\"detail\":\"Este tamanho acabou de esgotar.\"}"));
    }

    @Test
    void bffOwnRoutesShouldNeverBeForwarded() throws Exception {
        mvc.perform(get("/api/auth/qualquer-coisa")).andExpect(status().isNotFound());
        assertThat(LAST.get()).isNull();
    }

    @Test
    void slowApiShouldAnswer502WithAMessage() throws Exception {
        mvc.perform(get("/api/lento"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.detail").value("Não conseguimos concluir agora. Tente de novo em alguns instantes."));
    }

    private static HttpServer startFakeApi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", CommerceApiProxyIntegrationTest::answer);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String path = exchange.getRequestURI().getPath();
        LAST.set(new Received(exchange.getRequestMethod(), exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("Authorization"),
                exchange.getRequestHeaders().getFirst("X-Request-Id"),
                exchange.getRequestHeaders().getFirst("Cookie"), body));
        switch (path) {
            case "/lento" -> {
                sleep();
                send(exchange, 200, "{}");
            }
            case "/erro" -> send(exchange, 422, "{\"detail\":\"Este tamanho acabou de esgotar.\"}");
            case "/cart/items" -> {
                exchange.getResponseHeaders().add("Location", "/cart/items/1");
                send(exchange, 201, "{}");
            }
            default -> send(exchange, 200, "{\"ok\":true}");
        }
    }

    private static void send(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
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
