package br.com.george.commerce.bff.controller.me;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** /api/me/addresses contra uma commerce-api falsa: traduz os nomes e repassa os erros como vieram. */
@SpringBootTest(properties = "nani.auth.client-secret=segredo-de-teste")
class MeusEnderecosIntegrationTest {

    record Received(String method, String path, String authorization, String body) {
    }

    private static final AtomicReference<Received> LAST = new AtomicReference<>();
    private static final HttpServer API = startFakeApi();

    private static final String FRONT_BODY = """
            {"cep":"01310-100","street":"Avenida Paulista","number":"1000","complement":"",
             "district":"Bela Vista","city":"São Paulo","state":"SP","isDefault":true}""";

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

    private static MockHttpServletRequestBuilder asAna(MockHttpServletRequestBuilder request, String accessToken) {
        Instant now = Instant.now();
        return request
                .with(oidcLogin().idToken(token -> token.subject("ana-sub").claim("email", "ana@email.com")))
                .with(oauth2Client("nani").accessToken(new OAuth2AccessToken(
                        OAuth2AccessToken.TokenType.BEARER, accessToken, now, now.plusSeconds(300))));
    }

    @Test
    void withoutSessionShouldAnswer401AndNotCallTheApi() throws Exception {
        mvc.perform(get("/api/me/addresses")).andExpect(status().isUnauthorized());
        assertThat(LAST.get()).isNull();
    }

    @Test
    void listShouldComeInTheFrontNamesKeepingTheApiOrder() throws Exception {
        mvc.perform(asAna(get("/api/me/addresses"), "token-da-ana"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].cep").value("01310-100"))
                .andExpect(jsonPath("$[0].district").value("Bela Vista"))
                .andExpect(jsonPath("$[0].isDefault").value(true))
                .andExpect(jsonPath("$[0].complement").value(""))
                .andExpect(jsonPath("$[1].isDefault").value(false))
                .andExpect(jsonPath("$[0].zipCode").doesNotExist());
        assertThat(LAST.get().path()).isEqualTo("/me/addresses");
        assertThat(LAST.get().authorization()).isEqualTo("Bearer token-da-ana");
    }

    @Test
    void createShouldSendTheApiNamesAndAnswer201() throws Exception {
        mvc.perform(asAna(post("/api/me/addresses"), "token-da-ana").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(FRONT_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.isDefault").value(true));
        assertThat(LAST.get().method()).isEqualTo("POST");
        assertThat(LAST.get().body())
                .contains("\"zipCode\":\"01310-100\"")
                .contains("\"neighborhood\":\"Bela Vista\"")
                .contains("\"primaryAddress\":true");
    }

    @Test
    void updateAndRemoveShouldReachTheSameAddressInTheApi() throws Exception {
        mvc.perform(asAna(put("/api/me/addresses/7"), "token-da-ana").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(FRONT_BODY))
                .andExpect(status().isOk());
        assertThat(LAST.get().method()).isEqualTo("PUT");
        assertThat(LAST.get().path()).isEqualTo("/me/addresses/7");

        mvc.perform(asAna(delete("/api/me/addresses/7"), "token-da-ana").with(csrf()))
                .andExpect(status().isNoContent());
        assertThat(LAST.get().method()).isEqualTo("DELETE");
        assertThat(LAST.get().path()).isEqualTo("/me/addresses/7");
    }

    @Test
    void apiRuleErrorShouldReachTheFrontAsItCame() throws Exception {
        mvc.perform(asAna(post("/api/me/addresses"), "token-no-limite").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(FRONT_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADDRESS_LIMIT"));
    }

    @Test
    void writingShouldRequireCsrf() throws Exception {
        mvc.perform(asAna(delete("/api/me/addresses/7"), "token-da-ana")).andExpect(status().isForbidden());
        assertThat(LAST.get()).isNull();
    }

    private static HttpServer startFakeApi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", MeusEnderecosIntegrationTest::answer);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String method = exchange.getRequestMethod();
        LAST.set(new Received(method, exchange.getRequestURI().getPath(), authorization, body));
        String address = """
                {"id":7,"street":"Avenida Paulista","number":"1000","complement":null,"neighborhood":"Bela Vista",
                 "city":"São Paulo","state":"SP","zipCode":"01310-100","primaryAddress":true}""";
        if ("Bearer token-no-limite".equals(authorization)) {
            send(exchange, 409, "{\"status\":409,\"code\":\"ADDRESS_LIMIT\",\"detail\":\"Você chegou ao limite de 5 endereços. Remova um para cadastrar outro.\"}");
        } else if ("DELETE".equals(method)) {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        } else if ("GET".equals(method)) {
            send(exchange, 200, "[" + address + """
                    ,{"id":3,"street":"Rua B","number":"2","complement":"","neighborhood":"Centro",
                     "city":"Rio de Janeiro","state":"RJ","zipCode":"20040-002","primaryAddress":false}]""");
        } else {
            send(exchange, "POST".equals(method) ? 201 : 200, address);
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", status >= 400 ? "application/problem+json" : "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
