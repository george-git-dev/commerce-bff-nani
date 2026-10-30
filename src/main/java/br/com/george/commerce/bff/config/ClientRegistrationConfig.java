package br.com.george.commerce.bff.config;

import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;

/**
 * Cliente OIDC "nani" montado à mão em vez de descoberto pelo issuer-uri:
 * o BFF sobe mesmo com o Keycloak fora do ar (não consulta nada na inicialização),
 * e o navegador usa o endereço público enquanto o BFF usa o interno.
 */
@Configuration
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
}
