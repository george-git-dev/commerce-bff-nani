package br.com.george.commerce.bff.controller.me;

import br.com.george.commerce.bff.dto.MeResponse;
import br.com.george.commerce.bff.mapper.MeMapper;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** O Angular pergunta ao abrir o site: 200 com os dados, ou 401 se ninguém está logado. */
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
