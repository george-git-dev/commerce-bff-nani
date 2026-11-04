package br.com.george.commerce.bff.controller.me;

import br.com.george.commerce.bff.dto.EnderecoRequest;
import br.com.george.commerce.bff.dto.EnderecoResponse;
import br.com.george.commerce.bff.service.EnderecoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Área do Cliente - Endereços", description = "Endereços salvos do cliente logado.")
@RestController
@RequestMapping("/bff/me/addresses")
public class MeusEnderecosController {

    private final EnderecoService enderecoService;

    public MeusEnderecosController(EnderecoService enderecoService) {
        this.enderecoService = enderecoService;
    }

    @Operation(summary = "Listar meus endereços")
    @GetMapping
    public ResponseEntity<List<EnderecoResponse>> listar() {
        return ResponseEntity.ok(enderecoService.listar());
    }

    @Operation(summary = "Cadastrar endereço")
    @PostMapping
    public ResponseEntity<EnderecoResponse> cadastrar(@RequestBody EnderecoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(enderecoService.cadastrar(request));
    }

    /** Também serve para "Tornar principal" ({@code isDefault: true}). */
    @Operation(summary = "Atualizar endereço")
    @PutMapping("/{id}")
    public ResponseEntity<EnderecoResponse> atualizar(@PathVariable Long id, @RequestBody EnderecoRequest request) {
        return ResponseEntity.ok(enderecoService.atualizar(id, request));
    }

    @Operation(summary = "Remover endereço")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        enderecoService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
