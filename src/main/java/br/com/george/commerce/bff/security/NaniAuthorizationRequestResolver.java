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
 * - "prompt=create" abre direto a tela de cadastro (botão "Criar conta" do site);
 * - "kc_action=UPDATE_PASSWORD" / "UPDATE_PROFILE" abrem a troca de senha / de nome do Keycloak
 *   (Minha conta → Dados pessoais).
 * Só repassa valores das listas abaixo — nada que venha livre da URL.
 */
public class NaniAuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    public static final String BASE_URI = "/oauth2/authorization";
    private static final Set<String> ALLOWED_PROMPTS = Set.of("create");
    private static final Set<String> ALLOWED_ACTIONS = Set.of("UPDATE_PASSWORD", "UPDATE_PROFILE");

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
        String prompt = allowed(request.getParameter("prompt"), ALLOWED_PROMPTS);
        String action = allowed(request.getParameter("kc_action"), ALLOWED_ACTIONS);
        if (prompt == null && action == null) {
            return authorization;
        }
        return OAuth2AuthorizationRequest.from(authorization)
                .additionalParameters(params -> {
                    if (prompt != null) {
                        params.put("prompt", prompt);
                    }
                    if (action != null) {
                        params.put("kc_action", action);
                    }
                })
                .build();
    }

    private static String allowed(String value, Set<String> allowList) {
        return value != null && allowList.contains(value) ? value : null;
    }
}
