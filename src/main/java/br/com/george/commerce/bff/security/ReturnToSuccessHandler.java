package br.com.george.commerce.bff.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

/** Depois do login, volta para a página que o cliente estava (guardada em {@link ReturnTo}). */
public class ReturnToSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException {
        response.sendRedirect(ReturnTo.consume(request.getSession(false)));
    }
}
