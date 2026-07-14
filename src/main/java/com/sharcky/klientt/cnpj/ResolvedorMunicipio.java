package com.sharcky.klientt.cnpj;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Resolve nome de município → <b>código IBGE</b> (catálogo do IBGE em {@code municipios.csv}).
 *
 * <p>Necessário porque o filtro {@code municipio} da Minha Receita só funciona por código IBGE
 * (por nome é ignorado) — ver 'Nova integração.md'. Sem UF, só resolve nomes únicos no país.
 */
@Component
public class ResolvedorMunicipio {

    private final Map<String, String> porUfNome = new HashMap<>();   // "SP|BAURU" -> "3506003"
    private final Map<String, String> porNome = new HashMap<>();     // "BAURU" -> código (só se único)

    public ResolvedorMunicipio() {
        carregar();
    }

    private void carregar() {
        Set<String> ambiguos = new HashSet<>();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                new ClassPathResource("municipios/municipios.csv").getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = r.readLine()) != null) {
                String[] p = linha.split(";", 3);
                if (p.length < 3) {
                    continue;
                }
                String codigo = p[0].trim();
                String nome = normalizar(p[1]);
                String uf = p[2].trim().toUpperCase();
                if (codigo.isEmpty() || nome.isEmpty() || uf.isEmpty()) {
                    continue;
                }
                porUfNome.put(uf + "|" + nome, codigo);
                if (ambiguos.contains(nome)) {
                    continue;
                }
                if (porNome.putIfAbsent(nome, codigo) != null) {   // nome repetido em >1 UF → ambíguo
                    porNome.remove(nome);
                    ambiguos.add(nome);
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao carregar o catálogo de municípios", e);
        }
    }

    /**
     * Código IBGE para {@code nome} (+ {@code uf} opcional). Sem UF, só resolve nomes únicos no país.
     * Devolve vazio se não encontrar.
     */
    public Optional<String> codigo(String nome, String uf) {
        if (nome == null || nome.isBlank()) {
            return Optional.empty();
        }
        String n = normalizar(nome);
        if (uf != null && !uf.isBlank()) {
            return Optional.ofNullable(porUfNome.get(uf.trim().toUpperCase() + "|" + n));
        }
        return Optional.ofNullable(porNome.get(n));
    }

    static String normalizar(String s) {
        return Normalizer.normalize(s.trim().toUpperCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
    }
}
