package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpSession;

/**
 * Para onde o cliente volta depois do login (ex.: "/finalizar-compra").
 * Só aceita caminho do próprio site — nunca outro domínio (evita redirecionamento aberto).
 */
public final class ReturnTo {

    static final String SESSION_ATTRIBUTE = "nani.returnTo";
    static final String DEFAULT = "/";

    private ReturnTo() {
    }

    public static boolean isSafe(String path) {
        return path != null
                && path.startsWith("/")
                && !path.startsWith("//")
                && !path.contains("\\")
                && !path.contains("://")
                && path.length() <= 512;
    }

    public static void remember(HttpSession session, String path) {
        session.setAttribute(SESSION_ATTRIBUTE, isSafe(path) ? path : DEFAULT);
    }

    static String consume(HttpSession session) {
        if (session == null) {
            return DEFAULT;
        }
        Object path = session.getAttribute(SESSION_ATTRIBUTE);
        session.removeAttribute(SESSION_ATTRIBUTE);
        return path instanceof String value && isSafe(value) ? value : DEFAULT;
    }
}
