package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.config.CommerceApiProperties;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Leva uma chamada até a commerce-api e devolve a resposta como veio (status, corpo e tipo).
 * Só passam os cabeçalhos da lista: nada de cookie do navegador ou Authorization de fora —
 * o token é sempre o da sessão. Falha de rede ou prazo esgotado: {@code ResourceAccessException}.
 */
@Component
public class CommerceApiClient {

    /** Cabeçalhos do navegador que seguem para a API. */
    private static final List<String> REQUEST_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT, HttpHeaders.ACCEPT_LANGUAGE, "X-Request-Id");

    /** Cabeçalhos da API que voltam para o navegador. */
    private static final List<String> RESPONSE_HEADERS = List.of(
            HttpHeaders.CONTENT_TYPE, HttpHeaders.CACHE_CONTROL, HttpHeaders.LOCATION, HttpHeaders.RETRY_AFTER,
            "X-Request-Id");

    private final RestClient http;
    private final CommerceApiProperties api;

    public CommerceApiClient(RestClient commerceApiRestClient, CommerceApiProperties api) {
        this.http = commerceApiRestClient;
        this.api = api;
    }

    /**
     * @param pathAndQuery caminho na API com a query já codificada (ex.: {@code /products?marca=lattafa})
     * @param accessToken  token do cliente logado; vazio para visitante anônimo
     */
    public ResponseEntity<byte[]> exchange(HttpMethod method, String pathAndQuery, HttpHeaders incoming,
            byte[] body, Optional<String> accessToken) {
        RestClient.RequestBodySpec request = http.method(method)
                .uri(URI.create(api.url() + pathAndQuery))
                .headers(headers -> {
                    REQUEST_HEADERS.forEach(name -> copy(incoming, headers, name));
                    accessToken.ifPresent(headers::setBearerAuth);
                });
        if (body != null && body.length > 0) {
            request.body(body);
        }
        return request.exchange((clientRequest, response) -> {
            HttpHeaders headers = new HttpHeaders();
            RESPONSE_HEADERS.forEach(name -> copy(response.getHeaders(), headers, name));
            return ResponseEntity.status(response.getStatusCode())
                    .headers(headers)
                    .body(response.getBody().readAllBytes());
        });
    }

    private static void copy(HttpHeaders from, HttpHeaders to, String name) {
        List<String> values = from.get(name);
        if (values != null && !values.isEmpty()) {
            to.put(name, values);
        }
    }
}
