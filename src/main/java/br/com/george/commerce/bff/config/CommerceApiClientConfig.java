package br.com.george.commerce.bff.config;

import br.com.george.commerce.bff.client.MinhaContaClient;
import br.com.george.commerce.bff.security.ApiAccessToken;
import java.net.http.HttpClient;
import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/** Tokens do cliente logado e o cliente HTTP que leva esses tokens até a commerce-api. */
@Configuration
public class CommerceApiClientConfig {

    private static final String REQUEST_ID = "X-Request-Id";

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

    /**
     * Cliente HTTP da commerce-api, usado por todos os clients de {@code client/}: endereço base,
     * sem seguir redirecionamento, prazos curtos (A9) e, em toda chamada, o token da sessão e o
     * X-Request-Id que veio do front (para achar a mesma requisição nos logs dos dois serviços).
     */
    @Bean
    RestClient commerceApiRestClient(CommerceApiProperties api, ApiAccessToken accessToken) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(api.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(api.readTimeout());
        return RestClient.builder()
                .baseUrl(api.url())
                .requestFactory(requests)
                .requestInterceptor((request, body, execution) -> {
                    accessToken.current().ifPresent(request.getHeaders()::setBearerAuth);
                    currentRequestId().ifPresent(id -> request.getHeaders().set(REQUEST_ID, id));
                    return execution.execute(request, body);
                })
                .build();
    }

    /** Fábrica dos clients declarativos ({@code client/}), todos sobre o {@code commerceApiRestClient}. */
    @Bean
    HttpServiceProxyFactory commerceApiClients(RestClient commerceApiRestClient) {
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(commerceApiRestClient)).build();
    }

    @Bean
    MinhaContaClient minhaContaClient(HttpServiceProxyFactory commerceApiClients) {
        return commerceApiClients.createClient(MinhaContaClient.class);
    }

    private static Optional<String> currentRequestId() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return Optional.ofNullable(attributes.getRequest().getHeader(REQUEST_ID));
        }
        return Optional.empty();
    }
}
