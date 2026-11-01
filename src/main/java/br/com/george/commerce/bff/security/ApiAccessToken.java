package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.util.WebUtils;

/**
 * Access token do cliente logado para chamar a commerce-api (vazio para visitante anônimo), lido
 * da sessão da requisição atual. Se venceu, renova com o refresh token. A renovação é uma por sessão
 * de cada vez: o Keycloak troca o refresh token a cada uso (revokeRefreshToken), então duas
 * renovações ao mesmo tempo derrubariam a sessão. Falha na renovação sobe como
 * {@code OAuth2AuthorizationException} (tratada no GlobalExceptionHandler).
 */
@Component
public class ApiAccessToken {

    private final OAuth2AuthorizedClientManager authorizedClients;

    public ApiAccessToken(OAuth2AuthorizedClientManager authorizedClients) {
        this.authorizedClients = authorizedClients;
    }

    public Optional<String> current() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof OAuth2AuthenticationToken login)
                || !(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            return Optional.empty();
        }
        HttpServletRequest request = attributes.getRequest();
        HttpServletResponse response = attributes.getResponse();
        HttpSession session = request.getSession(false);
        if (session == null) {
            return Optional.empty();
        }
        OAuth2AuthorizeRequest authorize = OAuth2AuthorizeRequest
                .withClientRegistrationId(login.getAuthorizedClientRegistrationId())
                .principal(login)
                .attribute(HttpServletRequest.class.getName(), request)
                .attribute(HttpServletResponse.class.getName(), response)
                .build();
        OAuth2AuthorizedClient client;
        synchronized (WebUtils.getSessionMutex(session)) {
            client = authorizedClients.authorize(authorize);
        }
        return Optional.ofNullable(client).map(authorized -> authorized.getAccessToken().getTokenValue());
    }
}
