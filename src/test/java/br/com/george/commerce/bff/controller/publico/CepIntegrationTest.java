package br.com.george.commerce.bff.controller.publico;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * GET /bff/cep/{cep} contra ViaCEP e BrasilAPI falsos (um servidor HTTP do próprio Java nos dois
 * papéis). Cada teste diz o que cada serviço responde.
 */
@SpringBootTest(properties = "nani.auth.client-secret=segredo-de-teste")
class CepIntegrationTest {

    record Fake(int status, String body) {
    }

    private static final Map<String, Fake> RESPONSES = new ConcurrentHashMap<>();
    private static final Map<String, AtomicInteger> HITS = new ConcurrentHashMap<>();
    private static final HttpServer SERVICES = startFakeServices();

    @DynamicPropertySource
    static void urls(DynamicPropertyRegistry properties) {
        String url = "http://localhost:" + SERVICES.getAddress().getPort();
        properties.add("nani.cep.viacep-url", () -> url);
        properties.add("nani.cep.brasilapi-url", () -> url);
    }

    @AfterAll
    static void stop() {
        SERVICES.stop(0);
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        RESPONSES.clear();
        HITS.clear();
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static final String VIACEP = "/ws/01310100/json/";
    private static final String BRASILAPI = "/api/cep/v2/01310100";

    @Test
    void semLoginDevolveOEnderecoDoViaCep() throws Exception {
        RESPONSES.put(VIACEP, new Fake(200, """
                {"cep":"01310-100","logradouro":"Avenida Paulista","bairro":"Bela Vista",
                 "localidade":"São Paulo","uf":"SP"}"""));

        mvc.perform(get("/bff/cep/01310-100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cep").value("01310100"))
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.district").value("Bela Vista"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"));

        assertThat(hits(BRASILAPI)).isZero();
    }

    @Test
    void cepQueOViaCepNaoConheceDa404SemTentarABrasilApi() throws Exception {
        RESPONSES.put(VIACEP, new Fake(200, "{\"erro\":\"true\"}"));

        mvc.perform(get("/bff/cep/01310100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CEP_NOT_FOUND"))
                .andExpect(jsonPath("$.detail").value("Não encontramos esse CEP. Confira ou preencha o endereço."));

        assertThat(hits(BRASILAPI)).isZero();
    }

    @Test
    void viaCepForaDoArUsaABrasilApi() throws Exception {
        RESPONSES.put(VIACEP, new Fake(500, "erro"));
        RESPONSES.put(BRASILAPI, new Fake(200, """
                {"cep":"01310100","street":"Avenida Paulista","neighborhood":"Bela Vista",
                 "city":"São Paulo","state":"SP"}"""));

        mvc.perform(get("/bff/cep/01310100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.street").value("Avenida Paulista"))
                .andExpect(jsonPath("$.state").value("SP"));
    }

    @Test
    void viaCepForaDoArEBrasilApiSemOCepDa404() throws Exception {
        RESPONSES.put(VIACEP, new Fake(500, "erro"));
        RESPONSES.put(BRASILAPI, new Fake(404, "{\"message\":\"não encontrado\"}"));

        mvc.perform(get("/bff/cep/01310100"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CEP_NOT_FOUND"));
    }

    @Test
    void osDoisForaDoArDa503() throws Exception {
        RESPONSES.put(VIACEP, new Fake(500, "erro"));
        RESPONSES.put(BRASILAPI, new Fake(502, "erro"));

        mvc.perform(get("/bff/cep/01310100"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("SERVICE_UNAVAILABLE"));
    }

    @Test
    void cepComMenosDe8NumerosDa400SemChamarNinguem() throws Exception {
        mvc.perform(get("/bff/cep/0131010"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CEP_INVALID"))
                .andExpect(jsonPath("$.detail").value("CEP inválido. Confira os 8 números."));

        assertThat(HITS).isEmpty();
    }

    private static int hits(String path) {
        AtomicInteger count = HITS.get(path);
        return count == null ? 0 : count.get();
    }

    private static HttpServer startFakeServices() {
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
            server.createContext("/", exchange -> {
                String path = exchange.getRequestURI().getPath();
                HITS.computeIfAbsent(path, p -> new AtomicInteger()).incrementAndGet();
                Fake fake = RESPONSES.getOrDefault(path, new Fake(404, "{}"));
                byte[] body = fake.body().getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(fake.status(), body.length);
                exchange.getResponseBody().write(body);
                exchange.close();
            });
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            return server;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
