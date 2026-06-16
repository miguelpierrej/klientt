package com.sharcky.klientt.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.scraper.dto.CadastraisPayload;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Adaptador da API gerida de CNPJ por CNAE (CNPJá/Casa dos Dados).
 * Desligado por default (klientt.cnpj.enabled=false) → devolve vazio.
 *
 * <p>⚠️ O mapeamento da resposta ({@link RegistoCnpj}) é genérico e <b>precisa de ser
 * ajustado ao schema real do fornecedor escolhido</b> — tal como o cliente real do scraper,
 * ainda não foi validado contra uma API a sério (ligar chave + base-url e testar).
 */
@Service
public class ApiGeridaCnpjFonte implements FonteCnpj {

    private static final Logger log = LoggerFactory.getLogger(ApiGeridaCnpjFonte.class);

    private final ClienteCnpjProperties properties;
    private final RestClient restClient;

    public ApiGeridaCnpjFonte(ClienteCnpjProperties properties) {
        this.properties = properties;
        this.restClient = properties.isConfigurado()
                ? RestClient.builder().baseUrl(properties.getBaseUrl()).build()
                : null;
    }

    @Override
    public List<EmpresaPayload> buscarPorCnae(String cnae, String municipio, int limite) {
        if (!properties.isConfigurado()) {
            log.debug("FonteCnpj desligada (klientt.cnpj.enabled=false) — busca por CNAE ignorada");
            return List.of();
        }
        try {
            RespostaCnpj resposta = restClient.get()
                    .uri(uri -> uri.path("/office/search")
                            .queryParam("cnae", cnae)
                            .queryParam("municipio", municipio)
                            .queryParam("situacao", "ATIVA")
                            .queryParam("limite", limite)
                            .build())
                    .header("Authorization", "Bearer " + properties.getApiKey())
                    .retrieve()
                    .body(RespostaCnpj.class);

            if (resposta == null || resposta.empresas() == null) {
                return List.of();
            }
            return resposta.empresas().stream().map(this::toPayload).toList();
        } catch (Exception ex) {
            log.warn("Falha na busca CNPJ por CNAE (cnae={}, municipio={}): {}", cnae, municipio, ex.getMessage());
            return List.of();
        }
    }

    private EmpresaPayload toPayload(RegistoCnpj r) {
        CadastraisPayload cadastrais = new CadastraisPayload(
                r.razaoSocial(), r.nomeFantasia(), r.situacao(),
                null, null, null, null, r.cnaePrincipal(), null, null);
        String nome = (r.nomeFantasia() != null && !r.nomeFantasia().isBlank())
                ? r.nomeFantasia() : r.razaoSocial();
        return new EmpresaPayload(
                nome, r.cnpj(), r.telefone(), r.email(), null, r.municipio(),
                null, null, null, "receita", null, List.of(), cadastrais);
    }

    // --- Schema genérico da resposta (AJUSTAR ao fornecedor real) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaCnpj(List<RegistoCnpj> empresas) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RegistoCnpj(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String situacao,
            String municipio,
            String telefone,
            String email,
            String cnaePrincipal
    ) {
    }
}
