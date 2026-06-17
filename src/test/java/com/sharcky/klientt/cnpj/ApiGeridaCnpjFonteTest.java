package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGeridaCnpjFonteTest {

    @Test
    void desligadaDevolveVazioSemChamarApi() {
        ClienteCnpjProperties props = new ClienteCnpjProperties();   // enabled=false por default
        FonteCnpj fonte = new ApiGeridaCnpjFonte(props);

        assertThat(fonte.buscarPorCnae("9602-5/01", "São Paulo", 50)).isEmpty();
    }

    @Test
    void mapeiaRespostaDaCasaDosDadosEEnviaPedidoCorreto() throws Exception {
        AtomicReference<String> corpo = new AtomicReference<>();
        AtomicReference<String> apiKey = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v5/cnpj/pesquisa", exchange -> {
            apiKey.set(exchange.getRequestHeaders().getFirst("api-key"));
            corpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = RESPOSTA.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resposta.length);
            exchange.getResponseBody().write(resposta);
            exchange.close();
        });
        server.start();
        try {
            ClienteCnpjProperties props = new ClienteCnpjProperties();
            props.setEnabled(true);
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            props.setApiKey("a-minha-chave");
            FonteCnpj fonte = new ApiGeridaCnpjFonte(props);

            List<EmpresaPayload> leads = fonte.buscarPorCnae("9602-5/01", "São Paulo", 50);

            // Pedido: header api-key + CNAE só dígitos + município normalizado + situação ATIVA
            assertThat(apiKey.get()).isEqualTo("a-minha-chave");
            assertThat(corpo.get())
                    .contains("\"codigo_atividade_principal\":[\"9602501\"]")
                    .contains("\"municipio\":[\"sao paulo\"]")
                    .contains("\"situacao_cadastral\":[\"ATIVA\"]");

            // Mapeamento da resposta
            assertThat(leads).hasSize(1);
            EmpresaPayload e = leads.get(0);
            assertThat(e.nome()).isEqualTo("DANIELA CRISTINA DA SILVA ABREU 27002231871"); // fallback razão (sem fantasia)
            assertThat(e.cnpj()).isEqualTo("97551277000144");
            assertThat(e.telefone()).isEqualTo("11-55555555");
            assertThat(e.email()).isEqualTo("da43639@gmail.com");
            assertThat(e.cidade()).isEqualTo("SAO PAULO");
            assertThat(e.fonte()).isEqualTo("casadosdados");
            assertThat(e.cadastrais()).isNotNull();
            assertThat(e.cadastrais().razaoSocial()).isEqualTo("DANIELA CRISTINA DA SILVA ABREU 27002231871");
            assertThat(e.cadastrais().situacaoCadastral()).isEqualTo("ATIVA");
            assertThat(e.cadastrais().cnaePrincipal()).isEqualTo("Cabeleireiros, manicure e pedicure");
            assertThat(e.cadastrais().optanteMei()).isTrue();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void buscaPorNomeEnviaBuscaTextual() throws Exception {
        AtomicReference<String> corpo = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v5/cnpj/pesquisa", exchange -> {
            corpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = RESPOSTA.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resposta.length);
            exchange.getResponseBody().write(resposta);
            exchange.close();
        });
        server.start();
        try {
            ClienteCnpjProperties props = new ClienteCnpjProperties();
            props.setEnabled(true);
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            props.setApiKey("k");
            FonteCnpj fonte = new ApiGeridaCnpjFonte(props);

            List<EmpresaPayload> leads = fonte.buscarPorNome("Drogaria do Zé", "São Paulo", 10);

            assertThat(corpo.get())
                    .contains("\"busca_textual\":[{")
                    .contains("\"texto\":[\"Drogaria do Zé\"]")   // texto é array, valor cru (não normalizado)
                    .contains("\"tipo_busca\":\"radical\"")
                    .contains("\"municipio\":[\"sao paulo\"]");
            assertThat(leads).hasSize(1);
        } finally {
            server.stop(0);
        }
    }

    private static final String RESPOSTA = """
            {"total":1,"cnpjs":[{
              "cnpj":"97551277000144",
              "razao_social":"DANIELA CRISTINA DA SILVA ABREU 27002231871",
              "nome_fantasia":"",
              "situacao_cadastral":{"situacao_atual":"ATIVA","motivo":"SEM MOTIVO"},
              "endereco":{"logradouro":"RUA EXEMPLO","numero":"10","bairro":"CENTRO","uf":"SP","municipio":"SAO PAULO","ibge":{"latitude":-23.5,"longitude":-46.6}},
              "data_abertura":"2011-07-14T00:00:00Z",
              "capital_social":0,
              "porte_empresa":{"codigo":"01","descricao":"Microempresa"},
              "descricao_natureza_juridica":"EMPRESARIO INDIVIDUAL",
              "atividade_principal":{"codigo":"9602501","descricao":"Cabeleireiros, manicure e pedicure"},
              "mei":{"optante":true},
              "simples":{"optante":true},
              "contato_telefonico":[{"completo":"11-55555555","ddd":"11","numero":"55555555","tipo":"fixo"}],
              "contato_email":[{"email":"da43639@gmail.com","valido":true,"dominio":"gmail.com"}]
            }]}
            """;
}
