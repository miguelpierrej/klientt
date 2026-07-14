package com.sharcky.klientt.conta.seguranca;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Falha de login: distingue "email por confirmar" ({@link DisabledException}) de credenciais inválidas.
 * No primeiro caso manda para /login?naoVerificado&email=... (mostra aviso + reenvio); senão /login?error.
 */
public class VerificacaoFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof DisabledException) {
            String email = request.getParameter("username");
            String url = UriComponentsBuilder.fromPath("/login")
                    .queryParam("naoVerificado")
                    .queryParamIfPresent("email",
                            email == null || email.isBlank()
                                    ? java.util.Optional.empty()
                                    : java.util.Optional.of(java.net.URLEncoder.encode(email, StandardCharsets.UTF_8)))
                    .build(true).toUriString();
            getRedirectStrategy().sendRedirect(request, response, url);
            return;
        }
        getRedirectStrategy().sendRedirect(request, response, "/login?error");
    }
}
