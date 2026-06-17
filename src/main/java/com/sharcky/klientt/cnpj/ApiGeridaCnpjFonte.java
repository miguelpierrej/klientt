package com.sharcky.klientt.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.scraper.dto.CadastraisPayload;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fonte de descoberta por CNAE via <b>Casa dos Dados</b> (https://api.casadosdados.com.br).
 * Desligada por default (klientt.cnpj.enabled=false) → devolve vazio.
 *
 * <p>Pesquisa avançada: {@code POST /v5/cnpj/pesquisa?tipo_resultado=completo}, autenticação por
 * header {@code api-key}. Só o {@code tipo_resultado=completo} traz contactos
 * ({@code contato_telefonico}/{@code contato_email}). O CNAE vai só com dígitos e o município
 * normalizado (minúsculas, sem acentos), como a API exige. Cada busca consome saldo da conta.
 */
@Service
public class ApiGeridaCnpjFonte implements FonteCnpj {

    private static final Logger log = LoggerFactory.getLogger(ApiGeridaCnpjFonte.class);
    private static final int LIMITE_MAX = 1000;

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
        String codigo = soDigitos(cnae);
        if (codigo == null) {
            return List.of();
        }
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("codigo_atividade_principal", List.of(codigo));
        return pesquisar(filtros, municipio, limite, "cnae=" + cnae);
    }

    @Override
    public List<EmpresaPayload> buscarPorNome(String nome, String municipio, int limite) {
        if (!temTexto(nome)) {
            return List.of();
        }
        Map<String, Object> textual = new LinkedHashMap<>();
        textual.put("texto", List.of(nome.trim()));   // texto é array de strings
        textual.put("tipo_busca", "radical");
        textual.put("razao_social", true);
        textual.put("nome_fantasia", true);
        Map<String, Object> filtros = new LinkedHashMap<>();
        filtros.put("busca_textual", List.of(textual));
        return pesquisar(filtros, municipio, limite, "nome=" + nome);
    }

    /** Executa a pesquisa avançada (POST /v5/cnpj/pesquisa) com os filtros dados + comuns. */
    private List<EmpresaPayload> pesquisar(Map<String, Object> filtros, String municipio, int limite, String desc) {
        if (!properties.isConfigurado()) {
            log.debug("FonteCnpj desligada (klientt.cnpj.enabled=false) — busca ignorada");
            return List.of();
        }
        try {
            Map<String, Object> corpo = new LinkedHashMap<>(filtros);
            String mun = normalizar(municipio);
            if (mun != null) {
                corpo.put("municipio", List.of(mun));
            }
            corpo.put("situacao_cadastral", List.of("ATIVA"));
            corpo.put("limite", Math.max(1, Math.min(limite, LIMITE_MAX)));
            corpo.put("pagina", 1);

            RespostaCnpj resposta = restClient.post()
                    .uri(uri -> uri.path("/v5/cnpj/pesquisa").queryParam("tipo_resultado", "completo").build())
                    .header("api-key", properties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(RespostaCnpj.class);

            if (resposta == null || resposta.cnpjs() == null) {
                return List.of();
            }
            return resposta.cnpjs().stream().map(this::toPayload).toList();
        } catch (Exception ex) {
            log.warn("Falha na busca CNPJ ({}): {}", desc, ex.getMessage());
            return List.of();
        }
    }

    private EmpresaPayload toPayload(EmpresaCnpj e) {
        String nome = temTexto(e.nomeFantasia()) ? e.nomeFantasia() : e.razaoSocial();

        CadastraisPayload cadastrais = new CadastraisPayload(
                e.razaoSocial(),
                e.nomeFantasia(),
                e.situacaoCadastral() != null ? e.situacaoCadastral().situacaoAtual() : null,
                dataAbertura(e.dataAbertura()),
                e.capitalSocial(),
                e.porteEmpresa() != null ? e.porteEmpresa().descricao() : null,
                e.descricaoNaturezaJuridica(),
                e.atividadePrincipal() != null ? e.atividadePrincipal().descricao() : null,
                e.simples() != null ? e.simples().optante() : null,
                e.mei() != null ? e.mei().optante() : null);

        Double lat = null;
        Double lng = null;
        String endereco = null;
        String cidade = null;
        if (e.endereco() != null) {
            cidade = e.endereco().municipio();
            endereco = comporEndereco(e.endereco());
            if (e.endereco().ibge() != null) {
                lat = e.endereco().ibge().latitude();
                lng = e.endereco().ibge().longitude();
            }
        }

        return new EmpresaPayload(nome, e.cnpj(), primeiroTelefone(e), primeiroEmail(e),
                endereco, cidade, null, lat, lng, "casadosdados", null, List.of(), cadastrais);
    }

    private static String primeiroTelefone(EmpresaCnpj e) {
        if (e.contatoTelefonico() == null) {
            return null;
        }
        return e.contatoTelefonico().stream()
                .map(ContatoTel::completo)
                .filter(ApiGeridaCnpjFonte::temTexto)
                .findFirst().orElse(null);
    }

    /** Primeiro email, preferindo os marcados como válidos. */
    private static String primeiroEmail(EmpresaCnpj e) {
        if (e.contatoEmail() == null) {
            return null;
        }
        return e.contatoEmail().stream()
                .filter(c -> temTexto(c.email()))
                .sorted((a, b) -> Boolean.compare(Boolean.TRUE.equals(b.valido()), Boolean.TRUE.equals(a.valido())))
                .map(ContatoEmail::email)
                .findFirst().orElse(null);
    }

    private static String comporEndereco(Endereco e) {
        StringBuilder sb = new StringBuilder();
        juntar(sb, e.tipoLogradouro());
        juntar(sb, e.logradouro());
        juntar(sb, e.numero());
        juntar(sb, e.bairro());
        return sb.isEmpty() ? null : sb.toString();
    }

    private static void juntar(StringBuilder sb, String parte) {
        if (temTexto(parte)) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(parte.trim());
        }
    }

    private static LocalDate dataAbertura(String iso) {
        if (!temTexto(iso) || iso.length() < 10) {
            return null;
        }
        try {
            return LocalDate.parse(iso.substring(0, 10));
        } catch (Exception ex) {
            return null;
        }
    }

    private static boolean temTexto(String s) {
        return s != null && !s.isBlank();
    }

    private static String soDigitos(String cnae) {
        if (cnae == null) {
            return null;
        }
        String d = cnae.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static String normalizar(String s) {
        if (!temTexto(s)) {
            return null;
        }
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    // --- Resposta da Casa dos Dados (POST /v5/cnpj/pesquisa, tipo_resultado=completo) ---
    // snake_case via @JsonProperty (estável no Jackson 3 do Spring Boot 4).

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaCnpj(int total, List<EmpresaCnpj> cnpjs) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmpresaCnpj(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("situacao_cadastral") SituacaoCadastral situacaoCadastral,
            Endereco endereco,
            @JsonProperty("data_abertura") String dataAbertura,
            @JsonProperty("capital_social") BigDecimal capitalSocial,
            @JsonProperty("porte_empresa") CodigoDescricao porteEmpresa,
            @JsonProperty("descricao_natureza_juridica") String descricaoNaturezaJuridica,
            @JsonProperty("atividade_principal") CodigoDescricao atividadePrincipal,
            Optante mei,
            Optante simples,
            @JsonProperty("contato_telefonico") List<ContatoTel> contatoTelefonico,
            @JsonProperty("contato_email") List<ContatoEmail> contatoEmail) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record SituacaoCadastral(@JsonProperty("situacao_atual") String situacaoAtual) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Endereco(@JsonProperty("tipo_logradouro") String tipoLogradouro, String logradouro,
                    String numero, String bairro, String uf, String municipio, Ibge ibge) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Ibge(Double latitude, Double longitude) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CodigoDescricao(String codigo, String descricao) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Optante(Boolean optante) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContatoTel(String completo, String ddd, String numero, String tipo) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ContatoEmail(String email, Boolean valido, String dominio) {
    }
}
