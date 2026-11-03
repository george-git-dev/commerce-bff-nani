package br.com.george.commerce.bff.config;

import br.com.george.commerce.bff.security.ApiAccessToken;
import feign.RequestInterceptor;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Só do client da commerce-api ({@code configuration} do {@code @FeignClient}). SEM
 * {@code @Configuration} de propósito: se fosse global, o token do cliente iria também para o
 * ViaCEP e a BrasilAPI.
 */
public class CommerceApiFeignConfig {

    private static final String REQUEST_ID = "X-Request-Id";

    /** Token da sessão e o X-Request-Id do front (acha a mesma requisição nos logs dos dois serviços). */
    @Bean
    RequestInterceptor commerceApiHeaders(ApiAccessToken accessToken) {
        return template -> {
            accessToken.current().ifPresent(token -> template.header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
            currentRequestId().ifPresent(id -> template.header(REQUEST_ID, id));
        };
    }

    private static Optional<String> currentRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.ofNullable(attributes.getRequest().getHeader(REQUEST_ID));
        }
        return Optional.empty();
    }
}
