package br.com.george.commerce.bff.controller.publico;

import br.com.george.commerce.bff.config.ClientRegistrationConfig;
import br.com.george.commerce.bff.dto.SessaoResponse;
import br.com.george.commerce.bff.mapper.MeMapper;
import br.com.george.commerce.bff.security.NaniAuthorizationRequestResolver;
import br.com.george.commerce.bff.security.ReturnTo;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Portas de entrada do login para o Angular. Entrar e criar conta são navegação de página
 * inteira: guardam para onde voltar e mandam para o Keycloak. "Sair" é o POST
 * /api/auth/logout do Spring Security. A sessão é perguntada por XHR, sempre com 200.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final String AUTHORIZATION = NaniAuthorizationRequestResolver.BASE_URI + "/" + ClientRegistrationConfig.REGISTRATION_ID;

    private final MeMapper mapper;

    public AutenticacaoController(MeMapper mapper) {
        this.mapper = mapper;
    }

    /** Entrar: GET /api/auth/login?returnTo=/minha-conta */
    @GetMapping("/login")
    public RedirectView login(@RequestParam(required = false) String returnTo, HttpSession session) {
        ReturnTo.remember(session, returnTo);
        return new RedirectView(AUTHORIZATION, true);
    }

    /** Criar conta: abre direto o cadastro do Keycloak. */
    @GetMapping("/register")
    public RedirectView register(@RequestParam(required = false) String returnTo, HttpSession session) {
        ReturnTo.remember(session, returnTo);
        return new RedirectView(AUTHORIZATION + "?prompt=create", true);
    }

    /**
     * Tem alguém logado? O Angular pergunta ao abrir o site. Visitante anônimo não é erro:
     * responde 200 com authenticated=false (o user chega null), e 401 fica só para rota protegida.
     */
    @GetMapping("/session")
    public SessaoResponse session(@AuthenticationPrincipal OidcUser user) {
        return user == null ? SessaoResponse.anonima() : SessaoResponse.de(mapper.toResponse(user));
    }
}