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
 * Sem configuração ({@code klientt.email.enabled=false} ou sem api-key), NÃO envia: apenas registra
 * o link no log — assim o cadastro/confirmação/recuperação funciona em dev sem depender do Resend.
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
        String html = layout(
                "Bem-vindo, " + escape(nome) + "!",
                "Falta um passo para ativar sua conta: confirme seu e-mail.",
                "Confirmar e-mail",
                linkVerificacao,
                "O link é válido por 24 horas. Se você não criou esta conta, ignore este e-mail.");
        enviar(para, "Confirme seu e-mail — Klientt", html, "confirmação", linkVerificacao);
    }

    @Override
    public void enviarRecuperacaoSenha(String para, String nome, String linkReset) {
        String html = layout(
                "Olá, " + escape(nome),
                "Recebemos um pedido para redefinir sua senha. Clique no botão para escolher uma nova.",
                "Redefinir senha",
                linkReset,
                "O link é válido por 1 hora. Se você não solicitou, ignore este e-mail — sua senha continua a mesma.");
        enviar(para, "Redefinir sua senha — Klientt", html, "recuperação de senha", linkReset);
    }

    /** Envia (ou, sem Resend configurado, registra o link no log). Best-effort: nunca lança. */
    private void enviar(String para, String assunto, String html, String tipo, String link) {
        if (!props.isConfigurado()) {
            log.info("[EMAIL dev] Resend desligado — {} para {}: {}", tipo, para, link);
            return;
        }
        try {
            rest.post()
                    .uri("/emails")
                    .header("Authorization", "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "from", props.getFrom(),
                            "to", List.of(para),
                            "subject", assunto,
                            "html", html))
                    .retrieve()
                    .toBodilessEntity();
            log.info("Email de {} enviado para {}", tipo, para);
        } catch (Exception ex) {
            log.warn("Falha ao enviar email de {} para {}: {}", tipo, para, ex.getMessage());
        }
    }

    /**
     * Shell HTML no padrão visual do site (gradiente azul, logo Klientt, botão arredondado).
     * Usa substituição por marcadores ({@code {{...}}}) para não colidir com os '%' do CSS/gradiente.
     */
    private String layout(String saudacao, String intro, String textoBotao, String link, String nota) {
        return LAYOUT
                .replace("{{saudacao}}", saudacao)
                .replace("{{intro}}", intro)
                .replace("{{botao}}", textoBotao)
                .replace("{{nota}}", nota)
                .replace("{{link}}", link);   // href do botão, href e texto do fallback
    }

    private static final String LAYOUT = """
            <div style="display:none;max-height:0;overflow:hidden;opacity:0">{{intro}}</div>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f8fafc;padding:24px 0;font-family:system-ui,-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:600px;max-width:600px;background:#ffffff;border:1px solid #e2e8f0;border-radius:12px;overflow:hidden">
                  <tr><td bgcolor="#1e3a8a" style="background:linear-gradient(135deg,#2563eb 0%,#1e3a8a 100%);padding:32px 40px;text-align:center">
                    <span style="color:#ffffff;font-size:30px;font-weight:700;letter-spacing:-1px">Klientt</span>
                  </td></tr>
                  <tr><td style="padding:36px 40px 8px 40px">
                    <h1 style="margin:0 0 12px;font-size:22px;color:#0f172a">{{saudacao}}</h1>
                    <p style="margin:0 0 24px;font-size:15px;line-height:1.6;color:#334155">{{intro}}</p>
                    <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                      <td bgcolor="#2563eb" style="border-radius:8px">
                        <a href="{{link}}" style="display:inline-block;padding:13px 26px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:8px">{{botao}}</a>
                      </td>
                    </tr></table>
                    <p style="margin:26px 0 0;font-size:13px;line-height:1.6;color:#64748b">
                      Ou copie este link no navegador:<br>
                      <a href="{{link}}" style="color:#2563eb;word-break:break-all">{{link}}</a>
                    </p>
                    <p style="margin:18px 0 0;font-size:13px;color:#64748b">{{nota}}</p>
                  </td></tr>
                  <tr><td style="padding:24px 40px 32px;border-top:1px solid #e2e8f0;color:#94a3b8;font-size:12px;line-height:1.6">
                    &copy; Klientt &mdash; gera&ccedil;&atilde;o de leads B2B.
                  </td></tr>
                </table>
              </td></tr>
            </table>
            """;

    private static String escape(String s) {
        return s == null ? "" : s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
