package br.com.george.commerce.bff.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Conexão com o Keycloak (commerce-auth), realm da Nani.
 *
 * @param publicUrl  endereço que o navegador usa; também é o "issuer" dos tokens
 * @param internalUrl endereço usado pelo BFF por dentro (token, chaves, userinfo)
 */
@Validated
@ConfigurationProperties("nani.auth")
public record NaniAuthProperties(
        @NotBlank String publicUrl,
        @NotBlank String internalUrl,
        @NotBlank String realm,
        @NotBlank String clientId,
        @NotBlank String clientSecret,
        @NotBlank String redirectUri,
        @NotBlank String postLogoutRedirectUri) {

    public String issuer() {
        return publicUrl + "/realms/" + realm;
    }

    public String endSessionEndpoint() {
        return issuer() + "/protocol/openid-connect/logout";
    }

    String internalOidc(String path) {
        return internalUrl + "/realms/" + realm + "/protocol/openid-connect/" + path;
    }
}
