package br.com.george.commerce.bff.config;

import br.com.george.commerce.bff.security.KeycloakLogoutSuccessHandler;
import br.com.george.commerce.bff.security.NaniAuthorizationRequestResolver;
import br.com.george.commerce.bff.security.ReturnToSuccessHandler;
import br.com.george.commerce.bff.security.SpaCsrfTokenRequestHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;

/**
 * Segurança do BFF:
 * - login OIDC no Keycloak (código de autorização + PKCE); tokens só na sessão do servidor;
 * - o navegador recebe apenas o cookie NANI_SESSION (HttpOnly) e o XSRF-TOKEN;
 * - uma rota explícita por necessidade do front; quem exige login responde 401 (nunca redireciona);
 *   rota não declarada no BFF nunca chega à commerce-api.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ClientRegistrationRepository registrations,
            NaniAuthProperties auth) throws Exception {
        http
            .authorizeHttpRequests(requests -> requests
                // Públicos: login, cadastro, sessão, busca de CEP e a documentação. O resto exige sessão (401 sem ela).
                // Rota que não existe no BFF nunca chega à commerce-api (401 sem sessão, 404 com sessão).
                .requestMatchers("/actuator/health", "/api/auth/**", "/api/cep/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/v3/api-docs.yaml", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .oauth2Login(login -> login
                .authorizationEndpoint(endpoint -> endpoint
                    .baseUri(NaniAuthorizationRequestResolver.BASE_URI)
                    .authorizationRequestResolver(new NaniAuthorizationRequestResolver(registrations)))
                .successHandler(new ReturnToSuccessHandler()))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler(new KeycloakLogoutSuccessHandler(
                    auth.endSessionEndpoint(), auth.postLogoutRedirectUri())))
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .csrfTokenRequestHandler(new SpaCsrfTokenRequestHandler()))
            .exceptionHandling(errors -> errors
                .defaultAuthenticationEntryPointFor(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                    PathPatternRequestMatcher.withDefaults().matcher("/api/**")));
        return http.build();
    }
}
