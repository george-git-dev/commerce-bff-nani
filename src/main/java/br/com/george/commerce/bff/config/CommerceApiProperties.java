package br.com.george.commerce.bff.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Conexão com a commerce-api (servidor para servidor).
 *
 * @param url            endereço interno da API (em produção, a rede do Docker)
 * @param connectTimeout prazo para conectar (A9: API fora do ar não prende o BFF)
 * @param readTimeout    prazo para a resposta; menor que o do front (15 s)
 */
@Validated
@ConfigurationProperties("nani.api")
public record CommerceApiProperties(
        @NotBlank String url,
        @NotNull Duration connectTimeout,
        @NotNull Duration readTimeout) {
}
