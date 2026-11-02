package br.com.george.commerce.bff.controller.me;

import br.com.george.commerce.bff.dto.AtualizarPerfilRequest;
import br.com.george.commerce.bff.dto.PerfilResponse;
import br.com.george.commerce.bff.service.PerfilService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Área do Cliente - Minha Conta", description = "Dados da conta do cliente logado.")
@RestController
@RequestMapping("/api/me")
public class MeuPerfilController {

    private final PerfilService perfilService;

    public MeuPerfilController(PerfilService perfilService) {
        this.perfilService = perfilService;
    }

    /** Nome e e-mail do Keycloak + CPF (mascarado) e celular da commerce-api. */
    @Operation(summary = "Dados pessoais")
    @GetMapping("/profile")
    public ResponseEntity<PerfilResponse> perfil(@AuthenticationPrincipal OidcUser user) {
        return ResponseEntity.ok(perfilService.buscar(user));
    }

    @Operation(summary = "Salvar celular e CPF")
    @PatchMapping("/profile")
    public ResponseEntity<PerfilResponse> atualizar(@AuthenticationPrincipal OidcUser user,
            @RequestBody AtualizarPerfilRequest request) {
        return ResponseEntity.ok(perfilService.atualizar(user, request));
    }
}
