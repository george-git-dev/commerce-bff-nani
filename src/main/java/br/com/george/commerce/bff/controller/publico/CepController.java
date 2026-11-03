package br.com.george.commerce.bff.controller.publico;

import br.com.george.commerce.bff.dto.CepResponse;
import br.com.george.commerce.bff.service.CepService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Pública: a calculadora de frete do carrinho funciona sem login. CEP é dado público. */
@Tag(name = "Público - CEP", description = "Endereço a partir do CEP, para preencher formulários.")
@RestController
@RequestMapping("/api/cep")
public class CepController {

    private final CepService cepService;

    public CepController(CepService cepService) {
        this.cepService = cepService;
    }

    @Operation(summary = "Buscar endereço pelo CEP")
    @GetMapping("/{cep}")
    public ResponseEntity<CepResponse> buscar(@PathVariable String cep) {
        return ResponseEntity.ok(cepService.buscar(cep));
    }
}
