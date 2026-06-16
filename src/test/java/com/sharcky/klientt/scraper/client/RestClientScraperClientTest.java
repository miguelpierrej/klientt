package com.sharcky.klientt.scraper.client;

import com.sharcky.klientt.busca.dto.TipoBusca;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.EstadoScrape;
import com.sharcky.klientt.scraper.dto.ScrapeAck;
import com.sharcky.klientt.scraper.dto.ScrapeRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RestClientScraperClientTest {

    @Test
    void enviaPedidoComoJsonNoCorpo() throws Exception {
        AtomicReference<String> corpoRecebido = new AtomicReference<>();
        AtomicReference<String> protocoloRecebido = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/v1/scrapes", exchange -> {
            protocoloRecebido.set(exchange.getProtocol());
            corpoRecebido.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] resposta = """
                    {"scrapeId":"scrape-1","buscaId":"7","estado":"ACEITE"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(202, resposta.length);
            exchange.getResponseBody().write(resposta);
            exchange.close();
        });
        server.start();

        try {
            ScraperProperties properties = new ScraperProperties();
            properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            properties.setToken("dev-secret");
            RestClientScraperClient client = new RestClientScraperClient(RestClient.builder(), properties);

            ScrapeAck ack = client.iniciarBusca(new ScrapeRequest(
                    "7", TipoBusca.NICHO, "bares", "Lisboa", null, 30, 15, true, false,
                    "http://127.0.0.1:8080/api/scraper/callbacks"));

            assertThat(ack).isEqualTo(new ScrapeAck("scrape-1", "7", EstadoScrape.ACEITE));
            assertThat(protocoloRecebido.get()).isEqualTo("HTTP/1.1");
            assertThat(corpoRecebido.get())
                    .contains("\"buscaId\":\"7\"")
                    .contains("\"tamanhoLote\":15")
                    .contains("\"coletarEmails\":true")
                    .contains("\"callbackUrl\":\"http://127.0.0.1:8080/api/scraper/callbacks\"");
        } finally {
            server.stop(0);
        }
    }
}
