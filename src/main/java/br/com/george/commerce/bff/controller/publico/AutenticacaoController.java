package br.com.george.commerce.bff.controller.publico;

import br.com.george.commerce.bff.config.ClientRegistrationConfig;
import br.com.george.commerce.bff.security.NaniAuthorizationRequestResolver;
import br.com.george.commerce.bff.security.ReturnTo;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Portas de entrada do login para o Angular (navegação de página inteira, não XHR):
 * guarda para onde voltar e manda para o Keycloak. "Sair" é o POST /api/auth/logout do Spring Security.
 */
@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    private static final String AUTHORIZATION = NaniAuthorizationRequestResolver.BASE_URI + "/" + ClientRegistrationConfig.REGISTRATION_ID;

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
}
