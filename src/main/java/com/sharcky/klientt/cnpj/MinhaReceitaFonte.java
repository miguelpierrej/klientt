package com.sharcky.klientt.cnpj;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sharcky.klientt.cnpj.config.MinhaReceitaProperties;
import com.sharcky.klientt.cnpj.dto.CadastraisPayload;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.cnpj.dto.SocioPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Descoberta <b>gratuita</b> por CNAE via <b>Minha Receita</b> (https://minhareceita.org) — dados
 * públicos da Receita. {@code GET /?cnae=..&uf=..&municipio=..&limit=..&cursor=..} devolve
 * {@code {cursor, data[]}}; paginamos por cursor até ao {@code limite}.
 *
 * <p>Região: aceita "SP", "São Paulo" ou "São Paulo/SP" — 2 letras viram {@code uf}, o resto vira
 * {@code municipio} (por NOME, em maiúsculas sem acentos; por código a API dá timeout).
 * Não suporta busca por NOME (só CNAE/UF/município) — ver {@link #buscarPorNome}.
 */
@Service
public class MinhaReceitaFonte implements FonteCnpj {

    private static final Logger log = LoggerFactory.getLogger(MinhaReceitaFonte.class);

    /** Com filtro de cidade (código IBGE), a API aceita lotes maiores num só pedido (~7s). */
    private static final int LOTE_MAX_CIDADE = 100;

    private final MinhaReceitaProperties properties;
    private final ResolvedorMunicipio resolvedorMunicipio;
    private final RestClient restClient;

    public MinhaReceitaFonte(MinhaReceitaProperties properties, ResolvedorMunicipio resolvedorMunicipio) {
        this.properties = properties;
        this.resolvedorMunicipio = resolvedorMunicipio;
        this.restClient = properties.isEnabled()
                ? RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(fabrica()).build()
                : null;
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(45_000);   // o filtro por código IBGE é mais lento (~7s/pedido)
        return f;
    }

    @Override
    public List<EmpresaPayload> buscarPorCnae(String cnae, String municipio, int limite) {
        return buscarPaginaPorCnae(cnae, municipio, null, limite).empresas();
    }

    /**
     * Uma página a partir de {@code cursorInicial} (null = início). Consome páginas inteiras da API
     * (para o cursor devolvido ser limpo — não parte a meio de uma página). Devolve empresas + cursor
     * de continuação (null = esgotado).
     */
    @Override
    public Pagina buscarPaginaPorCnae(String cnae, String municipio, String cursorInicial, int tamanho) {
        if (!properties.isEnabled()) {
            return new Pagina(List.of(), null);
        }
        String codigoCnae = soDigitos(cnae);
        if (codigoCnae == null) {
            return new Pagina(List.of(), null);
        }
        String uf = ufDe(municipio);
        // Filtro por cidade é SERVER-SIDE via código IBGE (o 'municipio' da API por NOME é ignorado).
        String cidade = municipioDe(municipio);
        String codigoMun = cidade == null ? null : resolvedorMunicipio.codigo(cidade, uf).orElse(null);
        if (cidade != null && codigoMun == null) {
            log.debug("Cidade não resolvida para código IBGE: '{}' (uf={}) — busca só por UF", cidade, uf);
        }
        // Com código, a API devolve só a cidade e aceita lotes maiores; sem, páginas pequenas (senão timeout).
        int lote = codigoMun != null ? Math.min(tamanho, LOTE_MAX_CIDADE) : properties.getTamanhoPagina();

        List<EmpresaPayload> out = new ArrayList<>();
        String cursor = cursorInicial;
        int paginas = 0;
        try {
            while (out.size() < tamanho && paginas < properties.getMaxPaginas()) {
                final String cursorAtual = cursor;
                RespostaBusca resp = restClient.get()
                        .uri(b -> {
                            b.path("/").queryParam("cnae", codigoCnae).queryParam("limit", lote);
                            if (uf != null) b.queryParam("uf", uf);
                            if (codigoMun != null) b.queryParam("municipio", codigoMun);
                            if (cursorAtual != null) b.queryParam("cursor", cursorAtual);
                            return b.build();
                        })
                        .retrieve()
                        .body(RespostaBusca.class);
                paginas++;

                if (resp == null || resp.data() == null || resp.data().isEmpty()) {
                    cursor = null;   // esgotado
                    break;
                }
                resp.data().forEach(e -> out.add(toPayload(e)));   // consome a página inteira
                cursor = resp.cursor();
                if (cursor == null || cursor.isBlank()) {
                    break;   // esgotado
                }
            }
        } catch (Exception ex) {
            log.warn("Falha na descoberta Minha Receita (cnae={}, uf={}, cidade={}): {}", cnae, uf, cidade, ex.getMessage());
            cursor = null;
        }
        return new Pagina(out, (cursor == null || cursor.isBlank()) ? null : cursor);
    }

    /** O Minha Receita não faz busca textual por nome → vazio (o router encaminha NOME p/ Casa dos Dados). */
    @Override
    public List<EmpresaPayload> buscarPorNome(String nome, String municipio, int limite) {
        return List.of();
    }

    private EmpresaPayload toPayload(EmpresaMR e) {
        String nome = temTexto(e.nomeFantasia()) ? e.nomeFantasia() : e.razaoSocial();

        CadastraisPayload cadastrais = new CadastraisPayload(
                e.razaoSocial(), e.nomeFantasia(), e.situacao(), dataAbertura(e.dataInicioAtividade()),
                e.capitalSocial(), e.porte(), e.naturezaJuridica(), e.cnaeDescricao(),
                e.optanteSimples(), e.optanteMei());

        List<String> telefones = new ArrayList<>();
        juntar(telefones, e.dddTelefone1());
        juntar(telefones, e.dddTelefone2());
        List<String> emails = new ArrayList<>();
        juntar(emails, e.email());

        String telefone = telefones.isEmpty() ? null : telefones.get(0);
        String email = emails.isEmpty() ? null : emails.get(0);

        return new EmpresaPayload(nome, e.cnpj(), telefone, email,
                comporEndereco(e), e.municipio(), null, null, null, cadastrais, telefones, emails, socios(e));
    }

    /** Mapeia o QSA (sócios) da Receita. */
    private static List<SocioPayload> socios(EmpresaMR e) {
        if (e.qsa() == null) {
            return List.of();
        }
        List<SocioPayload> lista = new ArrayList<>();
        for (Qsa s : e.qsa()) {
            if (temTexto(s.nomeSocio())) {
                lista.add(new SocioPayload(s.nomeSocio().trim(), s.qualificacaoSocio(),
                        s.faixaEtaria(), dataAbertura(s.dataEntradaSociedade())));
            }
        }
        return lista;
    }

    private static String comporEndereco(EmpresaMR e) {
        StringBuilder sb = new StringBuilder();
        juntarEndereco(sb, e.tipoLogradouro());
        juntarEndereco(sb, e.logradouro());
        juntarEndereco(sb, e.numero());
        juntarEndereco(sb, e.bairro());
        return sb.isEmpty() ? null : sb.toString();
    }

    private static void juntarEndereco(StringBuilder sb, String parte) {
        if (temTexto(parte)) {
            if (!sb.isEmpty()) {
                sb.append(", ");
            }
            sb.append(parte.trim());
        }
    }

    private static void juntar(List<String> lista, String valor) {
        if (temTexto(valor) && !lista.contains(valor.trim())) {
            lista.add(valor.trim());
        }
    }

    // --- Região: "SP" | "São Paulo" | "São Paulo/SP" ---
    static String ufDe(String regiao) {
        for (String parte : partes(regiao)) {
            if (parte.length() == 2 && parte.chars().allMatch(Character::isLetter)) {
                return parte.toUpperCase();
            }
        }
        return null;
    }

    static String municipioDe(String regiao) {
        for (String parte : partes(regiao)) {
            if (!(parte.length() == 2 && parte.chars().allMatch(Character::isLetter))) {
                return normalizarMunicipio(parte);
            }
        }
        return null;
    }

    /** Município comparável: maiúsculas, sem acentos (como o Minha Receita armazena, ex.: "SAO PAULO"). */
    static String normalizarMunicipio(String s) {
        if (s == null) {
            return null;
        }
        return Normalizer.normalize(s.trim().toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private static List<String> partes(String regiao) {
        if (!temTexto(regiao)) {
            return List.of();
        }
        List<String> ps = new ArrayList<>();
        for (String p : regiao.trim().split("[/,]")) {
            if (temTexto(p)) {
                ps.add(p.trim());
            }
        }
        return ps;
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

    // --- Resposta do Minha Receita (dump da Receita; snake_case via @JsonProperty) ---

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RespostaBusca(String cursor, List<EmpresaMR> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmpresaMR(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            @JsonProperty("descricao_situacao_cadastral") String situacao,
            @JsonProperty("data_inicio_atividade") String dataInicioAtividade,
            @JsonProperty("capital_social") BigDecimal capitalSocial,
            String porte,
            @JsonProperty("natureza_juridica") String naturezaJuridica,
            @JsonProperty("cnae_fiscal_descricao") String cnaeDescricao,
            @JsonProperty("opcao_pelo_simples") Boolean optanteSimples,
            @JsonProperty("opcao_pelo_mei") Boolean optanteMei,
            @JsonProperty("ddd_telefone_1") String dddTelefone1,
            @JsonProperty("ddd_telefone_2") String dddTelefone2,
            String email,
            @JsonProperty("descricao_tipo_de_logradouro") String tipoLogradouro,
            String logradouro,
            String numero,
            String bairro,
            String municipio,
            String uf,
            String cep,
            List<Qsa> qsa) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Qsa(
            @JsonProperty("nome_socio") String nomeSocio,
            @JsonProperty("qualificacao_socio") String qualificacaoSocio,
            @JsonProperty("faixa_etaria") String faixaEtaria,
            @JsonProperty("data_entrada_sociedade") String dataEntradaSociedade) {
    }
}
