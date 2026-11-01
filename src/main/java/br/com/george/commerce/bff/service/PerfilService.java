package br.com.george.commerce.bff.service;

import br.com.george.commerce.bff.dto.PerfilResponse;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

public interface PerfilService {

    /** Dados pessoais do usuário logado (Keycloak + commerce-api). */
    PerfilResponse buscar(OidcUser user);
}
