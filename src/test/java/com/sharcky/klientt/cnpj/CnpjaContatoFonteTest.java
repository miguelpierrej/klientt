package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.CnpjaContatoProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CnpjaContatoFonteTest {

    @Test
    void desligadaDevolveVazio() {
        CnpjaContatoProperties props = new CnpjaContatoProperties();
        props.setEnabled(false);
        assertThat(new CnpjaContatoFonte(props).consultar("33683111000280").isVazio()).isTrue();
    }

    @Test
    void mapeiaEmailsEPhonesEChamaOEndpointCerto() throws Exception {
        AtomicInteger chamadas = new AtomicInteger();
        StringBuilder pathVisto = new StringBuilder();
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/office/", exchange -> {
            chamadas.incrementAndGet();
            pathVisto.append(exchange.getRequestURI().getPath());
            byte[] r = RESPOSTA.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, r.length);
            exchange.getResponseBody().write(r);
            exchange.close();
        });
        server.start();
        try {
            CnpjaContatoProperties props = new CnpjaContatoProperties();
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            props.setReqPorMinuto(6000);   // ~10ms de intervalo: não atrasa o teste
            FonteContatoCnpj.Contatos c = new CnpjaContatoFonte(props).consultar("47.960.950/0001-21");

            assertThat(pathVisto.toString()).isEqualTo("/office/47960950000121");   // só dígitos
            assertThat(c.emails()).containsExactly("fiscal.estadual@magazineluiza.com.br");
            assertThat(c.telefones()).containsExactly("1637112002");                 // area + number
            assertThat(chamadas.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void respeitaOThrottleEntreChamadas() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/office/", exchange -> {
            byte[] r = "{\"phones\":[],\"emails\":[]}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, r.length);
            exchange.getResponseBody().write(r);
            exchange.close();
        });
        server.start();
        try {
            CnpjaContatoProperties props = new CnpjaContatoProperties();
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            props.setReqPorMinuto(1200);   // intervalo 50ms
            CnpjaContatoFonte fonte = new CnpjaContatoFonte(props);

            long inicio = System.currentTimeMillis();
            fonte.consultar("11222333000181");   // 1ª sem espera
            fonte.consultar("11444777000161");   // 2ª espera ~50ms
            long decorrido = System.currentTimeMillis() - inicio;

            assertThat(decorrido).isGreaterThanOrEqualTo(40L);
        } finally {
            server.stop(0);
        }
    }

    private static final String RESPOSTA = """
            {"taxId":"47960950000121",
             "emails":[{"ownership":"CORPORATE","address":"fiscal.estadual@magazineluiza.com.br","domain":"magazineluiza.com.br"}],
             "phones":[{"type":"LANDLINE","area":"16","number":"37112002"}]}
            """;
}
