package br.com.george.commerce.bff.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Erros no mesmo formato da commerce-api ({@code ProblemDetail} com {@code code} e {@code detail} em
 * português). Os da API passam como vieram; aqui só se cria resposta para os erros do próprio BFF.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    static final String CODE = "code";
    static final String TRY_AGAIN = "Não conseguimos concluir agora. Tente de novo em alguns instantes.";

    /**
     * Erro da commerce-api (regra, validação, não encontrado…): passa para o front EXATAMENTE como
     * veio — mesmo status, mesmo corpo (code + detail). O BFF não reescreve nem engole.
     */
    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<byte[]> handleApiError(RestClientResponseException ex) {
        HttpHeaders apiHeaders = ex.getResponseHeaders();
        MediaType type = apiHeaders == null ? null : apiHeaders.getContentType();
        return ResponseEntity.status(ex.getStatusCode())
                .contentType(type == null ? MediaType.APPLICATION_PROBLEM_JSON : type)
                .body(ex.getResponseBodyAsByteArray());
    }

    /**
     * Renovação do token falhou. Recusada pelo Keycloak (refresh vencido ou revogado): a sessão acabou
     * de verdade — encerra aqui também e responde 401 (o front manda entrar de novo). Outra falha
     * (Keycloak fora do ar): 502, e a sessão continua.
     */
    @ExceptionHandler(OAuth2AuthorizationException.class)
    public ProblemDetail handleTokenRefresh(OAuth2AuthorizationException ex, HttpServletRequest request) {
        String reason = ex.getError().getErrorCode();
        if (OAuth2ErrorCodes.INVALID_GRANT.equals(reason) || "client_authorization_required".equals(reason)) {
            HttpSession session = request.getSession(false);
            if (session != null) {
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return problem(HttpStatus.UNAUTHORIZED, "SESSION_EXPIRED", "Sua sessão expirou. Entre de novo para continuar.");
        }
        log.warn("Falha ao renovar o token no Keycloak: {}", reason);
        return problem(HttpStatus.BAD_GATEWAY, "SERVICE_UNAVAILABLE", TRY_AGAIN);
    }

    /** commerce-api fora do ar ou lenta demais (prazos do RestClient). */
    @ExceptionHandler(ResourceAccessException.class)
    public ProblemDetail handleApiUnavailable(ResourceAccessException ex) {
        log.warn("commerce-api não respondeu: {}", ex.getMessage());
        return problem(HttpStatus.BAD_GATEWAY, "SERVICE_UNAVAILABLE", TRY_AGAIN);
    }

    /** Erro inesperado: detalhe só no log; para o cliente, a frase genérica. */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception ex) {
        log.error("Erro inesperado no BFF", ex);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", TRY_AGAIN);
    }

    /** Erros do Spring (rota inexistente, JSON malformado, método errado…) com frase em português. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = body instanceof ProblemDetail detail ? detail : ProblemDetail.forStatus(status);
        // Sem propriedades extras o Spring devolve null (não um mapa vazio).
        if (problem.getProperties() == null || !problem.getProperties().containsKey(CODE)) {
            switch (status.value()) {
                case 404 -> fill(problem, "NOT_FOUND", "Não encontramos o que você procurou.");
                case 405 -> fill(problem, "METHOD_NOT_ALLOWED", "Esta operação não é permitida aqui.");
                case 406, 415 -> fill(problem, "UNSUPPORTED_MEDIA_TYPE", "Formato de dados não aceito.");
                case 400 -> fill(problem, "BAD_REQUEST", "Confira os dados e tente de novo.");
                default -> fill(problem, status.is5xxServerError() ? "INTERNAL_ERROR" : "REQUEST_ERROR",
                        status.is5xxServerError() ? TRY_AGAIN : "Algo deu errado. Tente de novo.");
            }
        }
        return super.handleExceptionInternal(ex, problem, headers, status, request);
    }

    private static void fill(ProblemDetail problem, String code, String detail) {
        problem.setDetail(detail);
        problem.setProperty(CODE, code);
    }

    private static ProblemDetail problem(HttpStatus status, String code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty(CODE, code);
        return problem;
    }
}
