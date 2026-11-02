package br.com.george.commerce.bff.controller.auth;

import br.com.george.commerce.bff.config.ClientRegistrationConfig;
import br.com.george.commerce.bff.dto.SessaoResponse;
import br.com.george.commerce.bff.mapper.MeMapper;
import br.com.george.commerce.bff.security.NaniAuthorizationRequestResolver;
import br.com.george.commerce.bff.security.ReturnTo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import java.net.URI;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Portas de entrada do login para o Angular. Entrar e criar conta são navegação de página
 * inteira: guardam para onde voltar e mandam para o Keycloak. "Sair" é o POST
 * /api/auth/logout do Spring Security. A sessão é perguntada por XHR, sempre com 200.
 */
@Tag(name = "Público - Autenticação", description = "Entrar, criar conta e sessão. Sair: POST /api/auth/logout.")
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final String AUTHORIZATION = NaniAuthorizationRequestResolver.BASE_URI + "/" + ClientRegistrationConfig.REGISTRATION_ID;

    private final MeMapper mapper;

    public AutenticacaoController(MeMapper mapper) {
        this.mapper = mapper;
    }

    /** Entrar: GET /api/auth/login?returnTo=/minha-conta */
    @Operation(summary = "Entrar (vai para o Keycloak e volta para returnTo)")
    @GetMapping("/login")
    public ResponseEntity<Void> login(@RequestParam(required = false) String returnTo, HttpSession session) {
        ReturnTo.remember(session, returnTo);
        return redirect(AUTHORIZATION);
    }

    /** Criar conta: abre direto o cadastro do Keycloak. */
    @Operation(summary = "Criar conta (abre o cadastro do Keycloak)")
    @GetMapping("/register")
    public ResponseEntity<Void> register(@RequestParam(required = false) String returnTo, HttpSession session) {
        ReturnTo.remember(session, returnTo);
        return redirect(AUTHORIZATION + "?prompt=create");
    }

    /**
     * Tem alguém logado? O Angular pergunta ao abrir o site. Visitante anônimo não é erro:
     * responde 200 com authenticated=false (o user chega null), e 401 fica só para rota protegida.
     */
    @Operation(summary = "Trocar senha (abre a tela do Keycloak e volta para returnTo)")
    @GetMapping("/password")
    public ResponseEntity<Void> password(@RequestParam(required = false) String returnTo, HttpSession session) {
        ReturnTo.remember(session, returnTo);
        return redirect(AUTHORIZATION + "?kc_action=UPDATE_PASSWORD");
    }

    @Operation(summary = "Quem está logado (sempre 200)")
    @GetMapping("/session")
    public ResponseEntity<SessaoResponse> session(@AuthenticationPrincipal OidcUser user) {
        return ResponseEntity.ok(user == null ? SessaoResponse.anonima() : SessaoResponse.de(mapper.toResponse(user)));
    }

    /** Navegação de página inteira: 302 para a tela do Keycloak (via Spring Security). */
    private static ResponseEntity<Void> redirect(String path) {
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(path)).build();
    }
}
