package br.com.george.commerce.bff.dto;

/**
 * Resposta de GET /api/auth/session: diz se há alguém logado, sempre com 200.
 * Visitante anônimo não é erro; 401 fica só para rota protegida sem sessão.
 */
public record SessaoResponse(boolean authenticated, MeResponse user) {

    public static SessaoResponse anonima() {
        return new SessaoResponse(false, null);
    }

    public static SessaoResponse de(MeResponse user) {
        return new SessaoResponse(true, user);
    }
}
