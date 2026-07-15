package com.sharcky.klientt.cnae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Índice de sinónimos coloquiais → código CNAE (recurso {@code cnae/sinonimos.csv}). Cobre termos do
 * dia-a-dia ("dentista", "advogado", "restaurante") de forma determinística e rápida — <b>antes</b>
 * do LLM. Códigos são validados contra o catálogo no arranque; os inválidos são ignorados.
 */
@Component
public class SinonimoCnae {

    private static final Logger log = LoggerFactory.getLogger(SinonimoCnae.class);

    private final CnaeCatalogoRepository catalogo;
    /** sinónimo (normalizado + singular) → códigos CNAE válidos. */
    private volatile Map<String, List<String>> mapa;

    public SinonimoCnae(CnaeCatalogoRepository catalogo) {
        this.catalogo = catalogo;
    }

    /**
     * Códigos para um termo <b>completo</b> (resolução da busca): casa o sinónimo como palavra/frase
     * inteira (evita "bar" casar "barbearia"), tolerando plural.
     */
    public List<String> codigosPara(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String alvo = singularizar(normalizar(termo));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        indice().forEach((sinonimo, codigos) -> {
            if ((" " + alvo + " ").contains(" " + sinonimo + " ")) {
                out.addAll(codigos);
            }
        });
        return new ArrayList<>(out);
    }

    /**
     * Códigos para texto <b>parcial</b> (autocomplete): casa por substring (ex.: "denti" → dentista),
     * para sugerir enquanto se digita.
     */
    public List<String> codigosPorTexto(String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        String qn = singularizar(normalizar(q));
        LinkedHashSet<String> out = new LinkedHashSet<>();
        indice().forEach((sinonimo, codigos) -> {
            if (sinonimo.contains(qn) || qn.contains(sinonimo)) {
                out.addAll(codigos);
            }
        });
        return new ArrayList<>(out);
    }

    private Map<String, List<String>> indice() {
        Map<String, List<String>> m = mapa;
        if (m == null) {
            carregar();
            m = mapa;
        }
        return m;
    }

    private synchronized void carregar() {
        if (mapa != null) {
            return;
        }
        Set<String> codigosValidos = catalogo.findAll().stream()
                .map(CnaeCatalogo::getCodigo)
                .collect(Collectors.toSet());

        Map<String, List<String>> m = new LinkedHashMap<>();
        int ignorados = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource("cnae/sinonimos.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = r.readLine()) != null) {
                linha = linha.trim();
                if (linha.isEmpty() || linha.startsWith("#")) {
                    continue;
                }
                String[] p = linha.split(";", 2);
                if (p.length < 2) {
                    continue;
                }
                String sinonimo = singularizar(normalizar(p[0]));
                String codigo = p[1].replaceAll("\\D", "").trim();
                if (sinonimo.isEmpty() || codigo.isEmpty()) {
                    continue;
                }
                if (!codigosValidos.contains(codigo)) {
                    ignorados++;
                    log.warn("Sinónimo '{}' aponta para código CNAE inexistente no catálogo: {}", p[0], codigo);
                    continue;
                }
                m.computeIfAbsent(sinonimo, k -> new ArrayList<>()).add(codigo);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar cnae/sinonimos.csv", e);
        }
        log.info("Sinónimos CNAE carregados: {} termos ({} ignorados por código inválido)", m.size(), ignorados);
        this.mapa = m;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    /** Remove o 's' final de cada palavra (plural → singular, aproximado). */
    private static String singularizar(String s) {
        return s.replaceAll("s(?=\\s|$)", "");
    }
}
