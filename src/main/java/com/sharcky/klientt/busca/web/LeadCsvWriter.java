package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.RedeView;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gera o CSV dos leads. Delimitador ';' e BOM UTF-8 para abrir corretamente
 * (com acentos) no Excel em português.
 */
@Component
public class LeadCsvWriter {

    private static final char SEP = ';';
    private static final String[] CABECALHO = {
            "Nome", "Cidade", "CNPJ", "Telefone", "Email", "Contactável", "Endereço", "Website",
            "Nota Google", "Avaliações", "Tem Site", "Velocidade (ms)", "HTTPS",
            "Nº Páginas", "Reputação", "Seguidores", "Procon Evite", "Score", "Redes"
    };

    public byte[] escrever(List<LeadDetalhe> leads) {
        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // BOM UTF-8 (Excel pt-BR)
        linha(sb, CABECALHO);
        for (LeadDetalhe l : leads) {
            linha(sb,
                    l.nome(),
                    l.cidade(),
                    l.cnpj(),
                    l.telefone(),
                    l.email(),
                    l.contactavel() ? "Sim" : "Não",
                    l.endereco(),
                    l.website(),
                    texto(l.notaGoogle()),
                    texto(l.numReviews()),
                    simNao(l.siteExiste()),
                    texto(l.siteVelocidadeMs()),
                    simNao(l.siteHttps()),
                    texto(l.siteNumPaginas()),
                    l.siteReputacao(),
                    String.valueOf(totalSeguidores(l)),
                    l.proconEviteSite() ? "Sim" : "Não",
                    String.valueOf(l.score()),
                    redes(l));
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private void linha(StringBuilder sb, String... campos) {
        for (int i = 0; i < campos.length; i++) {
            if (i > 0) sb.append(SEP);
            sb.append(escapar(campos[i]));
        }
        sb.append("\r\n");
    }

    private String escapar(String valor) {
        if (valor == null || valor.isEmpty()) {
            return "";
        }
        boolean precisaAspas = valor.indexOf(SEP) >= 0 || valor.contains("\"")
                || valor.contains("\n") || valor.contains("\r");
        String v = valor.replace("\"", "\"\"");
        return precisaAspas ? "\"" + v + "\"" : v;
    }

    private String texto(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    private String simNao(Boolean b) {
        return b == null ? "" : (b ? "Sim" : "Não");
    }

    private int totalSeguidores(LeadDetalhe l) {
        return l.redes().stream().mapToInt(r -> r.seguidores() != null ? r.seguidores() : 0).sum();
    }

    private String redes(LeadDetalhe l) {
        return l.redes().stream()
                .map(this::descreverRede)
                .collect(Collectors.joining(", "));
    }

    private String descreverRede(RedeView r) {
        return r.rede() + (r.seguidores() != null ? "(" + r.seguidores() + ")" : "");
    }
}
