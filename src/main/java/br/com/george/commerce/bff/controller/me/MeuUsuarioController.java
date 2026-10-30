package br.com.george.commerce.bff.controller.me;

import br.com.george.commerce.bff.dto.MeResponse;
import br.com.george.commerce.bff.mapper.MeMapper;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Dados do usuário logado (rota protegida: 401 sem sessão). O Angular usa /api/auth/session ao abrir o site. */
@RestController
public class MeuUsuarioController {

    private final MeMapper mapper;

    public MeuUsuarioController(MeMapper mapper) {
        this.mapper = mapper;
    }

    @GetMapping("/api/me")
    public MeResponse me(@AuthenticationPrincipal OidcUser user) {
        return mapper.toResponse(user);
    }
}
