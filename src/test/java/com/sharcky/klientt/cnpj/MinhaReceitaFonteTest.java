package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.MinhaReceitaProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MinhaReceitaFonteTest {

    private static final ResolvedorMunicipio RESOLVEDOR = new ResolvedorMunicipio();

    @Test
    void desligadaDevolveVazio() {
        MinhaReceitaProperties props = new MinhaReceitaProperties();
        props.setEnabled(false);
        assertThat(new MinhaReceitaFonte(props, RESOLVEDOR).buscarPorCnae("4712-1/00", "SP", 50)).isEmpty();
    }

    @Test
    void buscaPorNomeSempreVazio() {
        assertThat(new MinhaReceitaFonte(new MinhaReceitaProperties(), RESOLVEDOR).buscarPorNome("Acme", "SP", 50)).isEmpty();
    }

    @Test
    void mapeiaEmpresaEEnviaCnaeUfMunicipio() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = servir(queries);
        try {
            MinhaReceitaFonte fonte = fonte(server);

            // Região "São Paulo/SP" → uf=SP + municipio=<código IBGE de São Paulo> (server-side).
            List<EmpresaPayload> leads = fonte.buscarPorCnae("4712-1/00", "São Paulo/SP", 1);

            assertThat(queries).hasSize(1);
            assertThat(queries.get(0))
                    .contains("cnae=4712100")
                    .contains("uf=SP")
                    .contains("municipio=3550308")     // código IBGE de São Paulo (filtro server-side)
                    .doesNotContain("cursor=");

            assertThat(leads).hasSize(1);
            EmpresaPayload e = leads.get(0);
            assertThat(e.nome()).isEqualTo("ACME");    // nome_fantasia
            assertThat(e.cnpj()).isEqualTo("63623958000159");
            assertThat(e.telefone()).isEqualTo("1140028922");
            assertThat(e.email()).isEqualTo("contato@acme.com.br");
            assertThat(e.cidade()).isEqualTo("SAO PAULO");
            assertThat(e.endereco()).isEqualTo("RUA, EXEMPLO, 10, CENTRO");
            assertThat(e.telefones()).containsExactly("1140028922");
            assertThat(e.cadastrais().razaoSocial()).isEqualTo("ACME LTDA");
            assertThat(e.cadastrais().situacaoCadastral()).isEqualTo("ATIVA");
            assertThat(e.cadastrais().cnaePrincipal()).isEqualTo("Comercio varejista");
            assertThat(e.cadastrais().porte()).isEqualTo("MICRO EMPRESA");
            assertThat(e.cadastrais().dataAbertura()).isEqualTo(LocalDate.of(2020, 5, 10));
            assertThat(e.cadastrais().optanteSimples()).isTrue();
            assertThat(e.cadastrais().optanteMei()).isFalse();
            assertThat(e.socios()).singleElement().satisfies(s -> {
                assertThat(s.nome()).isEqualTo("MARIA SILVA");
                assertThat(s.qualificacao()).isEqualTo("Socia-Administradora");
                assertThat(s.faixaEtaria()).isEqualTo("Entre 41 a 50 anos");
                assertThat(s.desde()).isEqualTo(LocalDate.of(2018, 3, 15));
            });
        } finally {
            server.stop(0);
        }
    }

    @Test
    void paginaSeguindoOCursorAteEsgotar() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = servir(queries);
        try {
            MinhaReceitaFonte fonte = fonte(server);

            List<EmpresaPayload> leads = fonte.buscarPorCnae("4712100", "SP", 10);

            // 2 páginas: pág1 (cursor=CURSOR2) → pág2 (cursor vazio) → pára.
            assertThat(queries).hasSize(2);
            assertThat(queries.get(0)).doesNotContain("cursor=");
            assertThat(queries.get(1)).contains("cursor=CURSOR2");
            assertThat(leads).extracting(EmpresaPayload::cnpj)
                    .containsExactly("63623958000159", "07398798000110");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void respeitaOLimite() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = servir(queries);
        try {
            // limite=1 → pára na 1ª página sem seguir o cursor.
            assertThat(fonte(server).buscarPorCnae("4712100", "SP", 1)).hasSize(1);
            assertThat(queries).hasSize(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void cidadeResolvidaVaiComoCodigoIbge() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = servirFixo(PAGINA_MISTA, queries);
        try {
            fonte(server).buscarPorCnae("5611201", "Bauru/SP", 5);
            // A API filtra por código IBGE (server-side); enviamos o código de Bauru.
            assertThat(queries.get(0)).contains("uf=SP").contains("municipio=3506003");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void cidadeDesconhecidaDegradaParaSoUf() throws Exception {
        List<String> queries = new ArrayList<>();
        HttpServer server = servirFixo(PAGINA_MISTA, queries);
        try {
            fonte(server).buscarPorCnae("5611201", "Cidadeinexistente/SP", 5);
            assertThat(queries.get(0)).contains("uf=SP").doesNotContain("municipio=");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void regiaoParseUfEMunicipio() {
        assertThat(MinhaReceitaFonte.ufDe("SP")).isEqualTo("SP");
        assertThat(MinhaReceitaFonte.ufDe("São Paulo/SP")).isEqualTo("SP");
        assertThat(MinhaReceitaFonte.ufDe("São Paulo")).isNull();
        assertThat(MinhaReceitaFonte.municipioDe("São Paulo/SP")).isEqualTo("SAO PAULO");
        assertThat(MinhaReceitaFonte.municipioDe("SP")).isNull();
    }

    // --- helpers ---
    private static MinhaReceitaFonte fonte(HttpServer server) {
        MinhaReceitaProperties props = new MinhaReceitaProperties();
        props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        return new MinhaReceitaFonte(props, RESOLVEDOR);
    }

    private static HttpServer servirFixo(String json, List<String> queries) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            queries.add(exchange.getRequestURI().getQuery());
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static final String PAGINA_MISTA = """
            {"cursor":"","data":[
              {"cnpj":"11111111000111","razao_social":"A SP","municipio":"SAO PAULO"},
              {"cnpj":"22222222000122","razao_social":"B CPS","municipio":"CAMPINAS"},
              {"cnpj":"33333333000133","razao_social":"C ST","municipio":"SANTOS"}
            ]}
            """;

    private static HttpServer servir(List<String> queries) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            String q = exchange.getRequestURI().getQuery();
            queries.add(q);
            String json = (q != null && q.contains("cursor=CURSOR2")) ? PAGINA2 : PAGINA1;
            byte[] resp = json.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resp.length);
            exchange.getResponseBody().write(resp);
            exchange.close();
        });
        server.start();
        return server;
    }

    private static final String PAGINA1 = """
            {"cursor":"CURSOR2","data":[{
              "cnpj":"63623958000159",
              "razao_social":"ACME LTDA","nome_fantasia":"ACME",
              "descricao_situacao_cadastral":"ATIVA","data_inicio_atividade":"2020-05-10",
              "capital_social":1000,"porte":"MICRO EMPRESA",
              "natureza_juridica":"206-2 - Sociedade Empresaria Limitada",
              "cnae_fiscal_descricao":"Comercio varejista",
              "opcao_pelo_simples":true,"opcao_pelo_mei":false,
              "ddd_telefone_1":"1140028922","ddd_telefone_2":"","email":"contato@acme.com.br",
              "descricao_tipo_de_logradouro":"RUA","logradouro":"EXEMPLO","numero":"10","bairro":"CENTRO",
              "municipio":"SAO PAULO","uf":"SP","cep":"01000000",
              "qsa":[{"nome_socio":"MARIA SILVA","qualificacao_socio":"Socia-Administradora","faixa_etaria":"Entre 41 a 50 anos","data_entrada_sociedade":"2018-03-15"}]
            }]}
            """;

    private static final String PAGINA2 = """
            {"cursor":"","data":[{
              "cnpj":"07398798000110",
              "razao_social":"BETA ME","nome_fantasia":"",
              "descricao_situacao_cadastral":"ATIVA","data_inicio_atividade":"2019-01-02",
              "capital_social":500,"porte":"MICRO EMPRESA","natureza_juridica":"213-5 - Empresario",
              "cnae_fiscal_descricao":"Comercio varejista","opcao_pelo_simples":true,"opcao_pelo_mei":true,
              "ddd_telefone_1":"","ddd_telefone_2":"","email":"",
              "descricao_tipo_de_logradouro":"AV","logradouro":"BRASIL","numero":"20","bairro":"JARDIM",
              "municipio":"SAO PAULO","uf":"SP","cep":"02000000"
            }]}
            """;
}
