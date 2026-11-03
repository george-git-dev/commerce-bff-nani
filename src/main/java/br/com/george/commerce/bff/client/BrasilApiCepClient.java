package br.com.george.commerce.bff.client;

import br.com.george.commerce.bff.dto.BrasilApiCepApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * BrasilAPI (brasilapi.com.br): reserva, se o ViaCEP falhar. CEP inexistente volta 404.
 * Sem token: nada do cliente logado sai para serviços de terceiros.
 */
@FeignClient(name = "brasilapi", url = "${nani.cep.brasilapi-url}")
public interface BrasilApiCepClient {

    @GetMapping("/api/cep/v2/{cep}")
    BrasilApiCepApiResponse buscar(@PathVariable("cep") String cep);
}
