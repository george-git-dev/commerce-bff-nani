package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Sair: a sessão do BFF já foi apagada; responde com o endereço de logout do Keycloak
 * (encerra a sessão lá também) em JSON. O Angular chama por XHR — com o cabeçalho CSRF —
 * e depois navega para esse endereço.
 */
public class KeycloakLogoutSuccessHandler implements LogoutSuccessHandler {

    private final String endSessionEndpoint;
    private final String postLogoutRedirectUri;

    public KeycloakLogoutSuccessHandler(String endSessionEndpoint, String postLogoutRedirectUri) {
        this.endSessionEndpoint = endSessionEndpoint;
        this.postLogoutRedirectUri = postLogoutRedirectUri;
    }

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        String target = postLogoutRedirectUri;
        if (authentication != null && authentication.getPrincipal() instanceof OidcUser user) {
            target = UriComponentsBuilder.fromUriString(endSessionEndpoint)
                    .queryParam("id_token_hint", user.getIdToken().getTokenValue())
                    .queryParam("post_logout_redirect_uri", postLogoutRedirectUri)
                    .encode()
                    .build()
                    .toUriString();
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"redirectUrl\":\"" + target.replace("\"", "%22") + "\"}");
    }
}
