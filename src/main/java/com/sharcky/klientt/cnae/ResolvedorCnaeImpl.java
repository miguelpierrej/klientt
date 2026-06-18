package com.sharcky.klientt.cnae;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve nicho→CNAE usando o catálogo CNAE do IBGE (tabela {@code cnae}):
 * <ol>
 *   <li>sinónimos coloquiais (ex.: "barbearia") → código, validados no catálogo;</li>
 *   <li>busca por descrição no catálogo (termos com redação oficial);</li>
 *   <li>fallback LLM (Claude), com o código devolvido validado no catálogo.</li>
 * </ol>
 * Devolve no máximo 1 CNAE (controla o saldo gasto na Casa dos Dados). Descrição sempre a oficial.
 */
@Service
public class ResolvedorCnaeImpl implements ResolvedorCnae {

    /** Sinónimos coloquiais → código CNAE (o catálogo dá a descrição oficial). */
    private static final Map<String, String> SINONIMOS = sinonimos();

    /** Nº máximo de candidatos a propor ao utilizador na confirmação do CNAE. */
    private static final int MAX_CANDIDATOS = 6;

    private final Optional<TradutorCnaeLlm> tradutor;
    private final CnaeCatalogoRepository catalogoRepository;
    private final Map<String, List<Cnae>> cache = new ConcurrentHashMap<>();
    private final Map<String, List<Cnae>> candidatosCache = new ConcurrentHashMap<>();

    /** Índice em memória do catálogo (carregado lazy na 1ª resolução). */
    private volatile List<Entrada> indice;
    private volatile Map<String, String> porCodigo;   // codigo → descrição oficial

    public ResolvedorCnaeImpl(Optional<TradutorCnaeLlm> tradutor, CnaeCatalogoRepository catalogoRepository) {
        this.tradutor = tradutor;
        this.catalogoRepository = catalogoRepository;
    }

    @Override
    public List<Cnae> resolver(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String alvo = normalizar(termo);

        // 1) Sinónimos coloquiais (validados no catálogo).
        for (Map.Entry<String, String> s : SINONIMOS.entrySet()) {
            if (alvo.contains(s.getKey())) {
                Cnae c = doCatalogo(s.getValue());
                if (c != null) {
                    return List.of(c);
                }
            }
        }

        // 2) Busca por descrição no catálogo.
        Cnae porDescricao = melhorPorDescricao(alvo);
        if (porDescricao != null) {
            return List.of(porDescricao);
        }

        // 3) Fallback LLM (com cache); valida o código no catálogo.
        return cache.computeIfAbsent(alvo, k -> tradutor
                .map(t -> t.traduzir(termo).stream()
                        .map(c -> doCatalogo(c.codigo()))
                        .filter(java.util.Objects::nonNull)
                        .limit(1)
                        .toList())
                .orElseGet(List::of));
    }

    @Override
    public List<Cnae> candidatos(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String alvo = normalizar(termo);
        return candidatosCache.computeIfAbsent(alvo, k -> montarCandidatos(termo, alvo));
    }

    /** Junta sinónimos + sugestão do LLM + correspondências do catálogo, deduplicado por código. */
    private List<Cnae> montarCandidatos(String termo, String alvo) {
        LinkedHashMap<String, Cnae> porCodigo = new LinkedHashMap<>();

        // 1) sinónimos coloquiais (alta confiança).
        for (Map.Entry<String, String> s : SINONIMOS.entrySet()) {
            if (alvo.contains(s.getKey())) {
                adicionar(porCodigo, doCatalogo(s.getValue()));
            }
        }
        // 2) sugestão do LLM (validada no catálogo).
        tradutor.ifPresent(t -> t.traduzir(termo)
                .forEach(c -> adicionar(porCodigo, doCatalogo(c.codigo()))));
        // 3) correspondências por descrição (top-N).
        for (Cnae c : candidatosPorDescricao(alvo, MAX_CANDIDATOS)) {
            adicionar(porCodigo, c);
        }
        return porCodigo.values().stream().limit(MAX_CANDIDATOS).toList();
    }

    private static void adicionar(LinkedHashMap<String, Cnae> mapa, Cnae c) {
        if (c != null) {
            mapa.putIfAbsent(c.codigo(), c);
        }
    }

    /** Entradas do catálogo ordenadas por nº de palavras do termo na descrição (depois mais curta). */
    private List<Cnae> candidatosPorDescricao(String alvo, int limite) {
        List<String> tokens = Arrays.stream(alvo.split("[^a-z0-9]+"))
                .filter(t -> t.length() >= 3)
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            return List.of();
        }
        record Pontuada(Entrada entrada, int score) {
        }
        List<Pontuada> pontuadas = new ArrayList<>();
        for (Entrada e : indice()) {
            int score = 0;
            for (String t : tokens) {
                if (e.descricaoNorm().contains(t)) {
                    score++;
                }
            }
            if (score >= 1) {
                pontuadas.add(new Pontuada(e, score));
            }
        }
        pontuadas.sort(Comparator.comparingInt((Pontuada p) -> p.score()).reversed()
                .thenComparingInt(p -> p.entrada().descricao().length()));
        return pontuadas.stream()
                .limit(limite)
                .map(p -> new Cnae(p.entrada().codigo(), p.entrada().descricao()))
                .toList();
    }

    /** Melhor entrada do catálogo por nº de palavras do termo presentes na descrição. */
    private Cnae melhorPorDescricao(String alvo) {
        List<String> tokens = Arrays.stream(alvo.split("[^a-z0-9]+"))
                .filter(t -> t.length() >= 3)
                .distinct()
                .toList();
        if (tokens.isEmpty()) {
            return null;
        }
        Entrada melhor = null;
        int melhorScore = 0;
        for (Entrada e : indice()) {
            int score = 0;
            for (String t : tokens) {
                if (e.descricaoNorm().contains(t)) {
                    score++;
                }
            }
            if (score > melhorScore
                    || (score == melhorScore && melhor != null && e.descricao().length() < melhor.descricao().length())) {
                if (score >= 1) {
                    melhor = e;
                    melhorScore = score;
                }
            }
        }
        return melhor != null ? new Cnae(melhor.codigo(), melhor.descricao()) : null;
    }

    /** Devolve o CNAE do catálogo (descrição oficial) ou {@code null} se o código não existir. */
    private Cnae doCatalogo(String codigo) {
        String c = soDigitos(codigo);
        if (c == null) {
            return null;
        }
        String descricao = porCodigo().get(c);
        return descricao != null ? new Cnae(c, descricao) : null;
    }

    private List<Entrada> indice() {
        List<Entrada> i = indice;
        if (i == null) {
            carregar();
            i = indice;
        }
        return i;
    }

    private Map<String, String> porCodigo() {
        Map<String, String> m = porCodigo;
        if (m == null) {
            carregar();
            m = porCodigo;
        }
        return m;
    }

    private synchronized void carregar() {
        if (indice != null) {
            return;
        }
        List<Entrada> entradas = new ArrayList<>();
        Map<String, String> mapa = new java.util.HashMap<>();
        for (CnaeCatalogo c : catalogoRepository.findAll()) {
            entradas.add(new Entrada(c.getCodigo(), c.getDescricao(), normalizar(c.getDescricao())));
            mapa.put(c.getCodigo(), c.getDescricao());
        }
        this.porCodigo = mapa;
        this.indice = entradas;
    }

    private static String soDigitos(String codigo) {
        if (codigo == null) {
            return null;
        }
        String d = codigo.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private static String normalizar(String s) {
        return Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }

    private record Entrada(String codigo, String descricao, String descricaoNorm) {
    }

    private static Map<String, String> sinonimos() {
        Map<String, String> m = new LinkedHashMap<>();
        m.put("barbearia", "9602501");
        m.put("salao de beleza", "9602502");
        m.put("pet shop", "4789004");
        m.put("petshop", "4789004");
        m.put("oficina mecanica", "4520001");
        m.put("lanchonete", "5611203");
        m.put("hamburgueria", "5611203");
        m.put("cafeteria", "5611203");
        m.put("supermercado", "4711302");
        m.put("mercado", "4712100");
        m.put("academia", "9313100");
        return m;
    }
}
