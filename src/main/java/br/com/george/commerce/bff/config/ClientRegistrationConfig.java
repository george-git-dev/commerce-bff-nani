package br.com.george.commerce.bff.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Cliente OIDC "nani" montado à mão em vez de descoberto pelo issuer-uri:
 * o BFF sobe mesmo com o Keycloak fora do ar (não consulta nada na inicialização),
 * e o navegador usa o endereço público enquanto o BFF usa o interno.
 */
@Configuration
@EnableConfigurationProperties(NaniAuthProperties.class)
public class ClientRegistrationConfig {

    public static final String REGISTRATION_ID = "nani";

    @Bean
    ClientRegistrationRepository clientRegistrationRepository(NaniAuthProperties auth) {
        ClientRegistration nani = ClientRegistration.withRegistrationId(REGISTRATION_ID)
                .clientName("Nani Perfumes")
                .clientId(auth.clientId())
                .clientSecret(auth.clientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(auth.redirectUri())
                .scope("openid", "profile", "email")
                .issuerUri(auth.issuer())
                .authorizationUri(auth.issuer() + "/protocol/openid-connect/auth")
                .tokenUri(auth.internalOidc("token"))
                .jwkSetUri(auth.internalOidc("certs"))
                .userInfoUri(auth.internalOidc("userinfo"))
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .providerConfigurationMetadata(Map.of("end_session_endpoint", auth.endSessionEndpoint()))
                .build();
        return new InMemoryClientRegistrationRepository(nani);
    }

    /**
     * Tokens guardados NA SESSÃO (A1): somem no logout e quando a sessão expira. Sem este bean,
     * o padrão do Spring guarda num mapa em memória por usuário que sobrevive ao logout.
     */
    @Bean
    OAuth2AuthorizedClientRepository authorizedClientRepository() {
        return new HttpSessionOAuth2AuthorizedClientRepository();
    }

    /** Entrega o access token válido; perto de vencer, renova com o refresh token no Keycloak. */
    @Bean
    OAuth2AuthorizedClientManager authorizedClientManager(ClientRegistrationRepository registrations,
            OAuth2AuthorizedClientRepository authorizedClients) {
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(registrations, authorizedClients);
        manager.setAuthorizedClientProvider(OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .build());
        return manager;
    }
}
