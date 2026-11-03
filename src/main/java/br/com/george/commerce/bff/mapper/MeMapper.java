package br.com.george.commerce.bff.mapper;

import br.com.george.commerce.bff.dto.MeResponse;

import java.util.List;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Component;

/** Usuário do Keycloak (claims do ID token / userinfo) → resposta do /api/me. */
@Component
public class MeMapper {

    public MeResponse toResponse(OidcUser user) {
        return MeResponse.builder()
                .id(user.getSubject())
                .name(name(user))
                .email(user.getEmail())
                .roles(appRoles(user.getClaimAsStringList("roles")))
                .permissions(listOrEmpty(user.getClaimAsStringList("permissions")))
                .build();
    }

    private String name(OidcUser user) {
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        String given = user.getGivenName() == null ? "" : user.getGivenName();
        String family = user.getFamilyName() == null ? "" : user.getFamilyName();
        String joined = (given + " " + family).trim();
        return joined.isEmpty() ? user.getEmail() : joined;
    }

    /** Só os papéis da loja (ROLE_*), sem os internos do Keycloak (default-roles-nani, offline_access…). */
    private List<String> appRoles(List<String> roles) {
        return roles == null ? List.of() : roles.stream().filter(role -> role.startsWith("ROLE_")).toList();
    }

    private List<String> listOrEmpty(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
