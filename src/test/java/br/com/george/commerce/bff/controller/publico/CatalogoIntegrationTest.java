package br.com.george.commerce.bff.controller.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oauth2Client;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** /bff/catalog contra uma commerce-api falsa: pública, sem token e só com os filtros conhecidos. */
@SpringBootTest(properties = "nani.auth.client-secret=segredo-de-teste")
class CatalogoIntegrationTest {

    record Received(String path, String query, String authorization) {
    }

    private static final AtomicReference<Received> LAST = new AtomicReference<>();
    private static final HttpServer API = startFakeApi();

    private static final String PRODUCT = """
            {"id":1,"slug":"lattafa-asad","line":"Asad","subtitle":null,"name":"Asad","description":"Oriental",
             "brandId":1,"brandName":"Lattafa","category":"perfume","gender":"Masculino",
             "concentration":"Eau de Parfum","families":["Oriental"],
             "notes":{"top":["Tabaco"],"heart":["Café"],"base":["Âmbar"]},"kitItems":[],"occasions":["noite"],
             "launch":false,"bestSeller":true,"badge":null,"soldCount":1456,"images":["img/produtos/a.webp"],
             "variants":[{"id":"lattafa-asad-100","kind":"frasco","volumeMl":100,"price":299.90,
                          "promoPrice":null,"stock":24}]}""";

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
    void searchShouldBePublicAndForwardOnlyTheKnownFilters() throws Exception {
        mvc.perform(get("/bff/catalog/products?category=perfume&category=kit&q=oud&sort=price_asc&page=0&hack=1&brand="))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].slug").value("lattafa-asad"))
                .andExpect(jsonPath("$.items[0].variants[0].price").value(299.9))
                .andExpect(jsonPath("$.totalItems").value(1));
        assertThat(LAST.get().path()).isEqualTo("/catalog/products");
        assertThat(LAST.get().query())
                .contains("category=perfume,kit")
                .contains("q=oud")
                .contains("sort=price_asc")
                .contains("page=0")
                .doesNotContain("hack")
                .doesNotContain("brand");
    }

    @Test
    void productShouldComeAsTheApiSent() throws Exception {
        mvc.perform(get("/bff/catalog/products/lattafa-asad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notes.base[0]").value("Âmbar"))
                .andExpect(jsonPath("$.variants[0].kind").value("frasco"));
        assertThat(LAST.get().path()).isEqualTo("/catalog/products/lattafa-asad");
    }

    @Test
    void unknownProductShouldReachTheFrontAsTheApiSent() throws Exception {
        mvc.perform(get("/bff/catalog/products/nao-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void loggedCustomerTokenShouldNotGoToTheShowcase() throws Exception {
        Instant now = Instant.now();
        mvc.perform(get("/bff/catalog/filters")
                        .with(oidcLogin().idToken(token -> token.subject("ana-sub")))
                        .with(oauth2Client("nani").accessToken(new OAuth2AccessToken(
                                OAuth2AccessToken.TokenType.BEARER, "token-da-ana", now, now.plusSeconds(300)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.brands[0]").value("Lattafa"));
        assertThat(LAST.get().authorization()).isNull();
    }

    private static HttpServer startFakeApi() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.setExecutor(Executors.newCachedThreadPool());
            server.createContext("/", CatalogoIntegrationTest::answer);
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }

    private static void answer(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String rawQuery = exchange.getRequestURI().getRawQuery();
        LAST.set(new Received(path, rawQuery == null ? "" : URLDecoder.decode(rawQuery, StandardCharsets.UTF_8),
                exchange.getRequestHeaders().getFirst("Authorization")));
        switch (path) {
            case "/catalog/products" -> send(exchange, 200,
                    "{\"items\":[" + PRODUCT + "],\"page\":0,\"size\":24,\"totalItems\":1,\"totalPages\":1}");
            case "/catalog/products/lattafa-asad" -> send(exchange, 200, PRODUCT);
            case "/catalog/filters" -> send(exchange, 200, """
                    {"categories":["perfume"],"brands":["Lattafa"],"families":["Oriental"],"occasions":["noite"],
                     "genders":["Masculino"],"badges":[],"minPrice":299.90,"maxPrice":299.90}""");
            default -> send(exchange, 404,
                    "{\"status\":404,\"code\":\"PRODUCT_NOT_FOUND\",\"detail\":\"Produto não encontrado.\"}");
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
