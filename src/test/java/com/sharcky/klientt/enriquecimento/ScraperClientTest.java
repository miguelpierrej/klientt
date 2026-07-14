package com.sharcky.klientt.enriquecimento;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.enriquecimento.config.ScraperProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ScraperClientTest {

    @Test
    void enviaPedidoComCorpoJsonEHeaderDeToken() throws Exception {
        AtomicReference<String> corpo = new AtomicReference<>("");
        AtomicReference<String> contentType = new AtomicReference<>();
        AtomicReference<String> token = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/enrich", exchange -> {
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            token.set(exchange.getRequestHeaders().getFirst("X-Klientt-Token"));
            corpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(202, -1);
            exchange.close();
        });
        server.start();
        try {
            ScraperProperties props = new ScraperProperties();
            props.setEnabled(true);
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            props.setToken("tkn");
            ScraperClient client = new ScraperClient(props);

            boolean ok = client.enriquecer(1L, List.of(new EmpresaPayload(
                    "Acme", "12345678000199", null, null, null, "SP", null, null, null, null,
                    List.of(), List.of(), List.of())));

            assertThat(ok).isTrue();
            assertThat(token.get()).isEqualTo("tkn");
            assertThat(contentType.get()).contains("application/json");
            assertThat(corpo.get())
                    .contains("\"buscaId\":\"1\"")
                    .contains("12345678000199");
        } finally {
            server.stop(0);
        }
    }
}
