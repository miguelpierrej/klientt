package com.sharcky.klientt.common.ratelimit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * Rate limiting por IP nos endpoints sensíveis (login/registo, e-mails, busca), para travar abuso:
 * brute-force, spam de e-mail (custo Resend) e uso das APIs externas pagas. Excedido → 429.
 * Corre no início da cadeia de segurança, antes da autenticação.
 */
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);
    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    /** Regra: método + caminho → política (capacidade por janela). Chave do balde = política + IP. */
    private record Regra(String metodo, String padrao, String politica, int capacidade, Duration janela) {}

    private static final List<Regra> REGRAS = List.of(
            // Brute-force de login.
            new Regra("POST", "/login", "login", 12, Duration.ofMinutes(5)),
            // Criação de contas em massa.
            new Regra("POST", "/registo", "registo", 5, Duration.ofMinutes(15)),
            // Envio de e-mail (custo + assédio): recuperação e reenvio de confirmação.
            new Regra("POST", "/recuperar-senha", "email", 5, Duration.ofMinutes(15)),
            new Regra("POST", "/verificar-email/reenviar", "email", 5, Duration.ofMinutes(15)),
            // Busca: dispara APIs externas pagas (Casa dos Dados, Gemini).
            new Regra("POST", "/buscar", "busca", 30, Duration.ofMinutes(1)));

    private final RateLimiter limiter;
    private final boolean enabled;

    public RateLimitFilter(RateLimiter limiter, boolean enabled) {
        this.limiter = limiter;
        this.enabled = enabled;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Regra regra = enabled ? regraPara(request) : null;
        if (regra != null) {
            String chave = regra.politica() + ":" + ipCliente(request);
            if (!limiter.tryAcquire(chave, regra.capacidade(), regra.janela())) {
                log.warn("Rate limit excedido: {} {} (política={}, ip={})",
                        request.getMethod(), request.getRequestURI(), regra.politica(), ipCliente(request));
                responder429(response, regra.janela());
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private Regra regraPara(HttpServletRequest request) {
        String metodo = request.getMethod();
        String caminho = request.getRequestURI();
        for (Regra r : REGRAS) {
            if (r.metodo().equals(metodo) && MATCHER.match(r.padrao(), caminho)) {
                return r;
            }
        }
        return null;
    }

    /** IP do cliente. Atrás de proxy (Railway) usa o 1º hop do X-Forwarded-For; senão, o remoto. */
    private static String ipCliente(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            int virgula = xff.indexOf(',');
            return (virgula > 0 ? xff.substring(0, virgula) : xff).trim();
        }
        return request.getRemoteAddr();
    }

    private static void responder429(HttpServletResponse response, Duration janela) throws IOException {
        response.setStatus(429);   // Too Many Requests
        response.setHeader("Retry-After", String.valueOf(janela.toSeconds()));
        response.setContentType("text/html; charset=UTF-8");
        response.getWriter().write("""
                <!doctype html><html lang="pt-br"><meta charset="utf-8">
                <body style="font-family:system-ui,sans-serif;text-align:center;padding:48px;color:#0f172a">
                  <h1 style="color:#dc2626">Muitas tentativas</h1>
                  <p>Você fez pedidos demais em pouco tempo. Aguarde alguns minutos e tente novamente.</p>
                  <p><a href="/login" style="color:#2563eb">Voltar ao login</a></p>
                </body></html>""");
    }
}
