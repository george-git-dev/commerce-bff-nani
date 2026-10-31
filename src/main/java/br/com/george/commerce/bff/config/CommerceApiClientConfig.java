package br.com.george.commerce.bff.config;

import java.net.http.HttpClient;
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

/** Tokens do cliente logado e o cliente HTTP que leva esses tokens até a commerce-api. */
@Configuration
public class CommerceApiClientConfig {

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

    /** Sem seguir redirecionamento e com prazos curtos (A9). */
    @Bean
    RestClient commerceApiRestClient(CommerceApiProperties api) {
        HttpClient http = HttpClient.newBuilder()
                .connectTimeout(api.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(http);
        requests.setReadTimeout(api.readTimeout());
        return RestClient.builder().requestFactory(requests).build();
    }
}
