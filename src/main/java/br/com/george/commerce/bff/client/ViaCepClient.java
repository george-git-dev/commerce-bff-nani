package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.dto.ViaCepApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * ViaCEP (viacep.com.br): busca principal. CEP inexistente volta 200 com {@code "erro": true}.
 * Sem token: nada do cliente logado sai para serviços de terceiros.
 */
@FeignClient(name = "viacep", url = "${nani.cep.viacep-url}")
public interface ViaCepClient {

    @GetMapping("/ws/{cep}/json/")
    ViaCepApiResponse buscar(@PathVariable("cep") String cep);
}
