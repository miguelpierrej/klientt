package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.ContatoFallbackProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BrasilApiContatoFonteTest {

    @Test
    void desligadaDevolveVazioSemChamarApi() {
        ContatoFallbackProperties props = new ContatoFallbackProperties();   // enabled=false por default
        FonteContatoCnpj fonte = new BrasilApiContatoFonte(props);

        assertThat(fonte.consultar("12345678000199").isVazio()).isTrue();
    }

    @Test
    void mapeiaTelefonesEEmailDaBrasilApi() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/cnpj/v1/12345678000199", exchange -> {
            byte[] resposta = """
                    {"ddd_telefone_1":"11 5555-5555","ddd_telefone_2":"11 4444-4444",
                     "email":"contato@empresa.com.br"}
                    """.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, resposta.length);
            exchange.getResponseBody().write(resposta);
            exchange.close();
        });
        server.start();
        try {
            ContatoFallbackProperties props = new ContatoFallbackProperties();
            props.setEnabled(true);
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            FonteContatoCnpj fonte = new BrasilApiContatoFonte(props);

            FonteContatoCnpj.Contatos c = fonte.consultar("12.345.678/0001-99");   // aceita máscara

            assertThat(c.telefones()).containsExactly("11 5555-5555", "11 4444-4444");
            assertThat(c.emails()).containsExactly("contato@empresa.com.br");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void erroDaApiDevolveVazio() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/cnpj/v1/", exchange -> {
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        try {
            ContatoFallbackProperties props = new ContatoFallbackProperties();
            props.setEnabled(true);
            props.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            FonteContatoCnpj fonte = new BrasilApiContatoFonte(props);

            assertThat(fonte.consultar("12345678000199").isVazio()).isTrue();   // falha graciosa
        } finally {
            server.stop(0);
        }
    }
}
