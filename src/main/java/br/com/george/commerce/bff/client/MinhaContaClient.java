package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.config.CommerceApiFeignConfig;
import br.com.george.commerce.bff.dto.AtualizarPerfilApiRequest;
import br.com.george.commerce.bff.dto.EnderecoApiRequest;
import br.com.george.commerce.bff.dto.EnderecoApiResponse;
import br.com.george.commerce.bff.dto.UsuarioApiResponse;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Rotas de "Minha conta" na commerce-api. O token da sessão e o X-Request-Id entram pelo
 * {@link CommerceApiFeignConfig}. Erro da API (4xx/5xx) sobe como {@code FeignException} e o front
 * recebe como veio.
 */
@FeignClient(name = "commerce-api", url = "${nani.api.url}", configuration = CommerceApiFeignConfig.class)
public interface MinhaContaClient {

    /** {@code GET /me}: perfil da loja do usuário do token. */
    @GetMapping("/me")
    UsuarioApiResponse buscarPerfil();

    /** {@code PATCH /me}: celular e CPF (o CPF só uma vez). */
    @PatchMapping("/me")
    UsuarioApiResponse atualizarPerfil(@RequestBody AtualizarPerfilApiRequest body);

    /** {@code GET /me/addresses}: principal primeiro. */
    @GetMapping("/me/addresses")
    List<EnderecoApiResponse> listarEnderecos();

    @PostMapping("/me/addresses")
    EnderecoApiResponse cadastrarEndereco(@RequestBody EnderecoApiRequest body);

    @PutMapping("/me/addresses/{id}")
    EnderecoApiResponse atualizarEndereco(@PathVariable("id") Long id, @RequestBody EnderecoApiRequest body);

    @DeleteMapping("/me/addresses/{id}")
    void removerEndereco(@PathVariable("id") Long id);
}
