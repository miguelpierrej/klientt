package com.sharcky.klientt.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sharcky.klientt.cnpj.config.ContatoFallbackProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Fallback de contacto por CNPJ via <b>BrasilAPI</b> ({@code GET /api/cnpj/v1/{cnpj}}) — dados da
 * Receita (telefones + email). Desligado por default (klientt.contato-fallback.enabled=false).
 *
 * <p>Nota: BrasilAPI e a Casa dos Dados bebem da mesma base da Receita, por isso o ganho é marginal —
 * mede-se a taxa de acerto antes de assumir valor (PLANO-SO-API.md, Fase D).
 */
@Service
public class BrasilApiContatoFonte implements FonteContatoCnpj {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiContatoFonte.class);

    private final ContatoFallbackProperties properties;
    private final RestClient restClient;

    public BrasilApiContatoFonte(ContatoFallbackProperties properties) {
        this.properties = properties;
        this.restClient = properties.isEnabled()
                ? RestClient.builder().baseUrl(properties.getBaseUrl()).build()
                : null;
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
        try {
            RespostaCnpj r = restClient.get()
                    .uri("/api/cnpj/v1/{cnpj}", digitos)
                    .retrieve()
                    .body(RespostaCnpj.class);
            if (r == null) {
                return Contatos.vazio();
            }
            List<String> telefones = new ArrayList<>();
            juntar(telefones, r.dddTelefone1());
            juntar(telefones, r.dddTelefone2());
            List<String> emails = new ArrayList<>();
            juntar(emails, r.email());
            return new Contatos(telefones, emails);
        } catch (Exception ex) {
            log.warn("Falha no fallback de contacto BrasilAPI cnpj={}: {}", digitos, ex.getMessage());
            return Contatos.vazio();
        }
    }

    private static void juntar(List<String> lista, String valor) {
        if (valor != null && !valor.isBlank() && !lista.contains(valor.trim())) {
            lista.add(valor.trim());
        }
    }

    private static String soDigitos(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        String d = cnpj.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaCnpj(
            @JsonProperty("ddd_telefone_1") String dddTelefone1,
            @JsonProperty("ddd_telefone_2") String dddTelefone2,
            String email) {
    }
}
