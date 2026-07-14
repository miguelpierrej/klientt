package com.sharcky.klientt.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sharcky.klientt.cnpj.config.CnpjaContatoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Enriquecimento de contacto por CNPJ via <b>CNPJá open</b> ({@code GET /office/{cnpj}}).
 * Shape rico: {@code emails[].address} e {@code phones[].area+number}.
 *
 * <p>Respeita o limite público de <b>5 req/min por IP</b> com um <i>throttle</i> global (espaça as
 * chamadas ~12 s). Corre no job {@code @Async} (background), por isso o atraso não afeta o utilizador.
 * Falha graciosa: erro/429/desligado → {@link Contatos#vazio()}.
 */
@Service
public class CnpjaContatoFonte implements FonteContatoCnpj {

    private static final Logger log = LoggerFactory.getLogger(CnpjaContatoFonte.class);

    private final CnpjaContatoProperties properties;
    private final RestClient restClient;
    private final long intervaloMs;

    private final Object throttleLock = new Object();
    private long proximoPermitidoMs = 0;

    public CnpjaContatoFonte(CnpjaContatoProperties properties) {
        this.properties = properties;
        this.intervaloMs = 60_000L / Math.max(1, properties.getReqPorMinuto());
        this.restClient = properties.isEnabled()
                ? RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(fabrica()).build()
                : null;
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(20_000);
        return f;
    }

    @Override
    public Contatos consultar(String cnpj) {
        if (!properties.isEnabled()) {
            return Contatos.vazio();
        }
        String digitos = soDigitos(cnpj);
        if (digitos == null) {
            return Contatos.vazio();
        }
        if (!aguardarVez()) {
            return Contatos.vazio();   // interrompido
        }
        try {
            RespostaOffice r = restClient.get()
                    .uri("/office/{cnpj}", digitos)
                    .retrieve()
                    .body(RespostaOffice.class);
            if (r == null) {
                return Contatos.vazio();
            }
            List<String> telefones = new ArrayList<>();
            for (Fone f : nullSafe(r.phones())) {
                String num = ((f.area() == null ? "" : f.area().trim()) + (f.number() == null ? "" : f.number().trim()));
                if (!num.isBlank() && !telefones.contains(num)) {
                    telefones.add(num);
                }
            }
            List<String> emails = new ArrayList<>();
            for (Mail m : nullSafe(r.emails())) {
                if (m.address() != null && !m.address().isBlank() && !emails.contains(m.address().trim())) {
                    emails.add(m.address().trim());
                }
            }
            return new Contatos(telefones, emails);
        } catch (Exception ex) {
            log.warn("Falha no enriquecimento CNPJá cnpj={}: {}", digitos, ex.getMessage());
            return Contatos.vazio();
        }
    }

    /** Espaça as chamadas para respeitar o teto por minuto (global entre threads). */
    private boolean aguardarVez() {
        long esperaMs;
        synchronized (throttleLock) {
            long agora = System.currentTimeMillis();
            long alvo = Math.max(agora, proximoPermitidoMs);
            esperaMs = alvo - agora;
            proximoPermitidoMs = alvo + intervaloMs;
        }
        if (esperaMs > 0) {
            try {
                Thread.sleep(esperaMs);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return true;
    }

    private static <T> List<T> nullSafe(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    private static String soDigitos(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        String d = cnpj.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    // --- Resposta do CNPJá open (/office/{cnpj}) ---
    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaOffice(List<Fone> phones, List<Mail> emails) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Fone(String type, String area, String number) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Mail(String ownership, String address, String domain) {
    }
}
