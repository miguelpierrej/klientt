package com.sharcky.klientt.enriquecimento.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.List;

/**
 * Callback do scraper com um lote de empresas enriquecidas (ver esquema em 'Novo Fluxo.md').
 * {@code estado} ∈ PARCIAL | CONCLUIDO | ERRO — nos dois últimos, o job é concluído.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record EnrichCallback(
        String buscaId,
        String estado,
        String erro,
        List<EmpresaEnriquecida> empresas
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EmpresaEnriquecida(
            String cnpj,
            String razaoSocial,
            String nomeFantasia,
            String website,
            List<Email> emails,
            List<Telefone> telefones,
            List<Rede> redes,
            Endereco endereco,
            GoogleMaps googleMaps,
            Cadastrais dadosCadastrais
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Email(String email, String fonte, Integer confianca) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Telefone(String telefone, String tipo, String fonte, Integer confianca) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Rede(String rede, String url) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Endereco(String logradouro, String numero, String bairro,
                           String cidade, String uf, String cep) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record GoogleMaps(String url, Double nota, Integer avaliacoes, String categoria, String horario) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Cadastrais(String cnae, String situacaoCadastral, String porte,
                             BigDecimal capitalSocial, String dataAbertura, String naturezaJuridica) {
    }
}
