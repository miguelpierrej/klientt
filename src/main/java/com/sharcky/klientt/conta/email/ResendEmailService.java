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

    /**
     * Shell do e-mail no visual editorial do site (índigo sobre papel quente, tinta). Tudo inline
     * (requisito de e-mail). A fonte display (Bricolage) é best-effort via @import — clientes que
     * não a suportam caem no fallback Arial; o layout e as cores carregam a marca de qualquer forma.
     */
    private static final String LAYOUT = """
            <style>@import url('https://fonts.googleapis.com/css2?family=Bricolage+Grotesque:opsz,wght@12..96,700;12..96,800&display=swap');</style>
            <div style="display:none;max-height:0;overflow:hidden;opacity:0">{{intro}}</div>
            <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:#f6f5f2;padding:28px 0;font-family:'Inter',system-ui,-apple-system,'Segoe UI',Roboto,Helvetica,Arial,sans-serif">
              <tr><td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="width:600px;max-width:600px;background:#ffffff;border:1px solid #e7e4dd;border-radius:20px;overflow:hidden">
                  <tr><td bgcolor="#3730a3" style="background:linear-gradient(135deg,#4f46e5 0%,#3730a3 100%);padding:34px 40px;text-align:center">
                    <span style="font-family:'Bricolage Grotesque','Helvetica Neue',Arial,sans-serif;color:#ffffff;font-size:30px;font-weight:800;letter-spacing:-1px">Klientt</span>
                  </td></tr>
                  <tr><td style="padding:38px 40px 8px 40px">
                    <h1 style="font-family:'Bricolage Grotesque','Helvetica Neue',Arial,sans-serif;margin:0 0 12px;font-size:24px;font-weight:700;letter-spacing:-.5px;color:#17151f">{{saudacao}}</h1>
                    <p style="margin:0 0 26px;font-size:15px;line-height:1.65;color:#4b4954">{{intro}}</p>
                    <table role="presentation" cellpadding="0" cellspacing="0"><tr>
                      <td bgcolor="#4f46e5" style="border-radius:999px">
                        <a href="{{link}}" style="display:inline-block;padding:14px 30px;font-size:15px;font-weight:600;color:#ffffff;text-decoration:none;border-radius:999px">{{botao}}</a>
                      </td>
                    </tr></table>
                    <p style="margin:28px 0 0;font-size:13px;line-height:1.6;color:#6b6a76">
                      Ou copie este link no navegador:<br>
                      <a href="{{link}}" style="color:#4f46e5;word-break:break-all">{{link}}</a>
                    </p>
                    <p style="margin:18px 0 0;font-size:13px;color:#6b6a76">{{nota}}</p>
                  </td></tr>
                  <tr><td style="padding:24px 40px 34px;border-top:1px solid #e7e4dd;color:#9a98a4;font-size:12px;line-height:1.6">
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
