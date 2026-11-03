package br.com.george.commerce.bff.dto;

import lombok.Builder;
/**
 * Resposta de GET /api/auth/session: diz se há alguém logado, sempre com 200.
 * Visitante anônimo não é erro; 401 fica só para rota protegida sem sessão.
 */
@Builder
public record SessaoResponse(boolean authenticated, MeResponse user) {

    public static SessaoResponse anonima() {
        return SessaoResponse.builder()
                .authenticated(false)
                .build();
    }

    public static SessaoResponse de(MeResponse user) {
        return SessaoResponse.builder()
                .authenticated(true)
                .user(user)
                .build();
    }
}
