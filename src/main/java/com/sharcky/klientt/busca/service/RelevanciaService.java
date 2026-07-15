package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.perfil.PerfilCliente;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

/**
 * Score de "fit" de um lead ao perfil (ICP) do cliente. Pontua só os critérios que o ICP define
 * (região, porte, ter contato, sem site, Simples/MEI) e devolve um rótulo relativo ao máximo possível.
 * Usado para o badge na lista e para ordenar por relevância.
 */
@Service
public class RelevanciaService {

    public record Fit(int pontos, int possiveis, String rotulo) {}

    public Fit avaliar(PerfilCliente perfil, Empresa e) {
        int pontos = 0;
        int possiveis = 0;

        if (!perfil.regioes().isEmpty()) {
            possiveis += 30;
            if (regiaoBate(perfil, e)) pontos += 30;
        }
        if (!perfil.portes().isEmpty()) {
            possiveis += 25;
            if (porteBate(perfil, e)) pontos += 25;
        }
        if (perfil.isQuerComContato()) {
            possiveis += 20;
            if (e.isContactavel()) pontos += 20;
        }
        if (perfil.isQuerSemSite()) {
            possiveis += 15;
            if (semSite(e)) pontos += 15;
        }
        if (perfil.isQuerSimplesMei()) {
            possiveis += 10;
            if (simplesOuMei(e)) pontos += 10;
        }
        return new Fit(pontos, possiveis, rotular(pontos, possiveis));
    }

    /** Pontos brutos (para a ordenação por relevância). */
    public int pontos(PerfilCliente perfil, Empresa e) {
        return avaliar(perfil, e).pontos();
    }

    private static String rotular(int pontos, int possiveis) {
        if (possiveis == 0) {
            return null;
        }
        double razao = (double) pontos / possiveis;
        if (razao >= 0.75) return "Ótimo fit";
        if (razao >= 0.40) return "Bom fit";
        return null;
    }

    private static boolean regiaoBate(PerfilCliente perfil, Empresa e) {
        String cidade = normalizar(e.getCidade());
        if (cidade.isEmpty()) {
            return false;
        }
        for (String alvo : perfil.regioes()) {
            // alvo pode ser "Cidade/UF" ou só UF; comparamos pela parte da cidade.
            String cidadeAlvo = normalizar(alvo.contains("/") ? alvo.substring(0, alvo.indexOf('/')) : alvo);
            if (!cidadeAlvo.isEmpty() && cidadeAlvo.equals(cidade)) {
                return true;
            }
        }
        return false;
    }

    private static boolean porteBate(PerfilCliente perfil, Empresa e) {
        String token = tokenPorte(e.getPorte());
        return token != null && perfil.portes().contains(token);
    }

    /** Mapeia o porte da Receita (ex.: "MICRO EMPRESA") para o token do ICP (MEI/MICRO/PEQUENA/GRANDE). */
    static String tokenPorte(String porte) {
        if (porte == null) {
            return null;
        }
        String p = normalizar(porte);
        if (p.contains("MEI")) return "MEI";
        if (p.contains("MICRO")) return "MICRO";
        if (p.contains("PEQUEN")) return "PEQUENA";
        if (p.isEmpty()) return null;
        return "GRANDE";   // "DEMAIS", "GRANDE", "MEDIO", etc.
    }

    private static boolean semSite(Empresa e) {
        return e.getWebsite() == null || e.getWebsite().isBlank();
    }

    private static boolean simplesOuMei(Empresa e) {
        return Boolean.TRUE.equals(e.getOptanteSimples()) || Boolean.TRUE.equals(e.getOptanteMei());
    }

    private static String normalizar(String s) {
        if (s == null) {
            return "";
        }
        return Normalizer.normalize(s.trim().toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
