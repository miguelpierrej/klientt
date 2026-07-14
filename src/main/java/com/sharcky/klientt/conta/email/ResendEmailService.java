package com.sharcky.klientt.conta.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

/**
 * Envio de email via Resend (https://resend.com — POST /emails).
 * Sem configuração ({@code klientt.email.enabled=false} ou sem api-key), NÃO envia: apenas regista
 * o link no log — assim o registo/confirmação funciona em dev sem depender do Resend.
 */
@Service
public class ResendEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    private final EmailProperties props;
    private final RestClient rest;

    public ResendEmailService(EmailProperties props) {
        this.props = props;
        this.rest = RestClient.builder().baseUrl(props.getBaseUrl()).build();
    }

    @Override
    public void enviarVerificacao(String para, String nome, String linkVerificacao) {
        if (!props.isConfigurado()) {
            log.info("[EMAIL dev] Resend desligado — confirmação de {} ({}): {}", para, nome, linkVerificacao);
            return;
        }
        String html = corpoHtml(nome, linkVerificacao);
        try {
            rest.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", props.getFrom(),
                            "to", List.of(para),
                            "subject", "Confirma o teu email — Klientt",
                            "html", html))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email de confirmação enviado para {}", para);
        } catch (Exception ex) {
            // Best-effort: uma falha de envio não deve rebentar o registo — o utilizador pode reenviar.
            log.warn("Falha ao enviar email de confirmação para {}: {}", para, ex.getMessage());
        }
    }

    private String corpoHtml(String nome, String link) {
        return """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:480px;margin:auto">
                  <h2 style="color:#111">Bem-vindo à Klientt, %s!</h2>
                  <p>Confirma o teu email para ativar a conta:</p>
                  <p style="margin:24px 0">
                    <a href="%s" style="background:#2563eb;color:#fff;padding:12px 20px;border-radius:8px;text-decoration:none">Confirmar email</a>
                  </p>
                  <p style="color:#555;font-size:13px">Ou copia este link: <br>%s</p>
                  <p style="color:#888;font-size:12px">Se não criaste esta conta, ignora este email.</p>
                </div>
                """.formatted(escape(nome), link, link);
    }

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
