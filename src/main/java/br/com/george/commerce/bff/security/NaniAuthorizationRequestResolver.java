package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Set;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestCustomizers;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

/**
 * Pedido de login ao Keycloak:
 * - sempre com PKCE (mesmo sendo cliente confidencial);
 * - "prompt=create" abre direto a tela de cadastro (botão "Criar conta" do site).
 * Só repassa valores da lista abaixo — nada que venha livre da URL.
 */
public class NaniAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String BASE_URI = "/oauth2/authorization";
    private static final Set<String> ALLOWED_PROMPTS = Set.of("create");

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public NaniAuthorizationRequestResolver(ClientRegistrationRepository registrations) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(registrations, BASE_URI);
        this.delegate.setAuthorizationRequestCustomizer(OAuth2AuthorizationRequestCustomizers.withPkce());
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(request, delegate.resolve(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String registrationId) {
        return customize(request, delegate.resolve(request, registrationId));
    }

    private OAuth2AuthorizationRequest customize(HttpServletRequest request, OAuth2AuthorizationRequest authorization) {
        if (authorization == null) {
            return null;
        }
        String prompt = request.getParameter("prompt");
        if (prompt == null || !ALLOWED_PROMPTS.contains(prompt)) {
            return authorization;
        }
        return OAuth2AuthorizationRequest.from(authorization)
                .additionalParameters(params -> params.put("prompt", prompt))
                .build();
    }
}
