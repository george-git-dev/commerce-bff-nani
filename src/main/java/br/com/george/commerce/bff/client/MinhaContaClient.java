package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.dto.UsuarioApiResponse;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Rotas de "Minha conta" na commerce-api, como HTTP Service Client do Spring (o substituto
 * recomendado do Feign): só a interface; o Spring cria a implementação em CommerceApiClientConfig.
 * O token da sessão, o X-Request-Id e os prazos vêm do {@code commerceApiRestClient}. Erro da API
 * (4xx/5xx) sobe como {@code RestClientResponseException} e o front recebe como veio.
 */
@HttpExchange("/me")
public interface MinhaContaClient {

    /** {@code GET /me}: perfil da loja do usuário do token. */
    @GetExchange
    UsuarioApiResponse buscarPerfil();
}
