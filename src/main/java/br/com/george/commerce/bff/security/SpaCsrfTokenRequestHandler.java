package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.function.Supplier;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.util.StringUtils;

/**
 * CSRF para o Angular (A2), no padrão da documentação do Spring Security para SPA:
 * o token vai no cookie XSRF-TOKEN e o HttpClient do Angular o devolve no cabeçalho
 * X-XSRF-TOKEN; formulários continuam usando o valor mascarado (proteção BREACH).
 */
public final class SpaCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

    private final CsrfTokenRequestHandler plain = new CsrfTokenRequestAttributeHandler();
    private final CsrfTokenRequestHandler xor = new XorCsrfTokenRequestAttributeHandler();

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, Supplier<CsrfToken> csrfToken) {
        this.xor.handle(request, response, csrfToken);
        // Gera o cookie já na primeira resposta (o Angular precisa dele antes do primeiro POST).
        csrfToken.get();
    }

    @Override
    public String resolveCsrfTokenValue(HttpServletRequest request, CsrfToken csrfToken) {
        String header = request.getHeader(csrfToken.getHeaderName());
        return (StringUtils.hasText(header) ? this.plain : this.xor).resolveCsrfTokenValue(request, csrfToken);
    }
}
