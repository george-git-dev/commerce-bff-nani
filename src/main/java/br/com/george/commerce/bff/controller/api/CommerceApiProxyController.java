package br.com.george.commerce.bff.controller.api;

import br.com.george.commerce.bff.client.CommerceApiClient;
import br.com.george.commerce.bff.security.ApiAccessToken;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Tudo em /api/** (menos /api/auth/**, que é do próprio BFF) vai para a commerce-api sem o
 * prefixo: GET /api/products?marca=x → GET {api}/products?marca=x, com o token da sessão
 * (visitante anônimo vai sem token e a API decide). Quem autoriza é a API; o BFF só garante
 * sessão, CSRF e o token.
 * <p>
 * Corte do MVP: repasse direto. Telas que precisarem juntar ou adaptar respostas ganham rotas
 * próprias aqui (client/ + service/), como previsto no CLAUDE.md.
 */
@RestController
public class CommerceApiProxyController {

    private static final Logger log = LoggerFactory.getLogger(CommerceApiProxyController.class);

    private static final String PREFIX = "/api";

    /** Renovação recusada pelo Keycloak: a sessão acabou de verdade. */
    private static final Set<String> SESSION_ENDED = Set.of(OAuth2ErrorCodes.INVALID_GRANT, "client_authorization_required");

    private final CommerceApiClient api;
    private final ApiAccessToken accessToken;

    public CommerceApiProxyController(CommerceApiClient api, ApiAccessToken accessToken) {
        this.api = api;
        this.accessToken = accessToken;
    }

    @RequestMapping("/api/**")
    public ResponseEntity<byte[]> forward(HttpMethod method, HttpServletRequest request, HttpServletResponse response,
            @RequestBody(required = false) byte[] body) {
        String path = request.getRequestURI().substring(request.getContextPath().length() + PREFIX.length());
        if (path.startsWith("/auth/") || path.equals("/auth")) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String query = request.getQueryString();
        Optional<String> token = accessToken.current(request, response);
        return api.exchange(method, query == null ? path : path + "?" + query, headersOf(request), body, token);
    }

    /** Sessão vencida no Keycloak: encerra aqui também e o Angular manda entrar de novo. */
    @ExceptionHandler(OAuth2AuthorizationException.class)
    ResponseEntity<ProblemDetail> tokenRefreshFailed(OAuth2AuthorizationException error, HttpServletRequest request) {
        if (SESSION_ENDED.contains(error.getError().getErrorCode())) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return problem(HttpStatus.UNAUTHORIZED, "Sua sessão expirou. Entre de novo para continuar.");
        }
        log.warn("Falha ao renovar o token no Keycloak: {}", error.getError().getErrorCode());
        return problem(HttpStatus.BAD_GATEWAY, "Não conseguimos concluir agora. Tente de novo em alguns instantes.");
    }

    /** API fora do ar ou lenta demais. */
    @ExceptionHandler(ResourceAccessException.class)
    ResponseEntity<ProblemDetail> apiUnavailable(ResourceAccessException error) {
        log.warn("commerce-api não respondeu: {}", error.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "Não conseguimos concluir agora. Tente de novo em alguns instantes.");
    }

    private static HttpHeaders headersOf(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        for (String name : Collections.list(request.getHeaderNames())) {
            headers.put(name, Collections.list(request.getHeaders(name)));
        }
        return headers;
    }

    private static ResponseEntity<ProblemDetail> problem(HttpStatus status, String detail) {
        return ResponseEntity.status(status).body(ProblemDetail.forStatusAndDetail(status, detail));
    }
}
