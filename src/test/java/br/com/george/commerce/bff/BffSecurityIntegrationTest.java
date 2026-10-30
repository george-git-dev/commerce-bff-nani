package br.com.george.commerce.bff;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Sobe o BFF de verdade, sem Keycloak (ele não é chamado na inicialização nem nos testes). */
@SpringBootTest(properties = "nani.auth.client-secret=segredo-de-teste")
class BffSecurityIntegrationTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void shouldAnswer401WhenNobodyIsLoggedIn() throws Exception {
        mvc.perform(get("/api/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturnNameRolesAndPermissionsOfTheLoggedUser() throws Exception {
        mvc.perform(get("/api/me").with(oidcLogin().idToken(token -> token
                        .subject("user-123")
                        .claim("given_name", "Ana")
                        .claim("family_name", "Souza")
                        .claim("email", "ana@email.com")
                        .claim("roles", List.of("ROLE_CUSTOMER", "default-roles-nani", "offline_access"))
                        .claim("permissions", List.of("shop:checkout")))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("user-123"))
                .andExpect(jsonPath("$.name").value("Ana Souza"))
                .andExpect(jsonPath("$.email").value("ana@email.com"))
                .andExpect(jsonPath("$.roles.length()").value(1))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_CUSTOMER"))
                .andExpect(jsonPath("$.permissions[0]").value("shop:checkout"));
    }

    @Test
    void loginShouldRememberWhereToReturnAndGoToKeycloak() throws Exception {
        mvc.perform(get("/api/auth/login").param("returnTo", "/minha-conta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", "/oauth2/authorization/nani"))
                .andExpect(request().sessionAttribute("nani.returnTo", "/minha-conta"));
    }

    @Test
    void loginShouldIgnoreReturnToOutsideTheSite() throws Exception {
        mvc.perform(get("/api/auth/login").param("returnTo", "https://evil.com"))
                .andExpect(request().sessionAttribute("nani.returnTo", "/"));
    }

    @Test
    void authorizationShouldUsePkceAndOpenRegistrationOnlyWhenAsked() throws Exception {
        String register = mvc.perform(get("/oauth2/authorization/nani").param("prompt", "create"))
                .andExpect(status().is3xxRedirection())
                .andReturn().getResponse().getHeader("Location");
        assertThat(register)
                .startsWith("http://localhost:8180/auth/realms/nani/protocol/openid-connect/auth")
                .contains("code_challenge=", "code_challenge_method=S256", "prompt=create");

        String other = mvc.perform(get("/oauth2/authorization/nani").param("prompt", "none"))
                .andReturn().getResponse().getHeader("Location");
        assertThat(other).doesNotContain("prompt=");
    }

    @Test
    void logoutShouldRequireCsrf() throws Exception {
        mvc.perform(post("/api/auth/logout").with(oidcLogin())).andExpect(status().isForbidden());
    }

    @Test
    void logoutShouldReturnTheKeycloakLogoutAddress() throws Exception {
        // Como o Angular faz: devolve no cabeçalho o mesmo valor do cookie XSRF-TOKEN.
        mvc.perform(post("/api/auth/logout").with(oidcLogin())
                        .cookie(new Cookie("XSRF-TOKEN", "token-de-teste"))
                        .header("X-XSRF-TOKEN", "token-de-teste"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.redirectUrl").value(org.hamcrest.Matchers.allOf(
                        org.hamcrest.Matchers.startsWith(
                                "http://localhost:8180/auth/realms/nani/protocol/openid-connect/logout"),
                        org.hamcrest.Matchers.containsString("id_token_hint="),
                        org.hamcrest.Matchers.containsString("post_logout_redirect_uri="))));
    }
}
