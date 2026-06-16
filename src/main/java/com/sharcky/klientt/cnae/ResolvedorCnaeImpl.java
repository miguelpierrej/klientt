package com.sharcky.klientt.cnae;

import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Resolve nicho→CNAE: tabela determinística para os nichos comuns (instantâneo,
 * grátis) e, no que falhar, fallback para o LLM (com cache do resultado).
 */
@Service
public class ResolvedorCnaeImpl implements ResolvedorCnae {

    /** Tabela curada (keyword normalizada → CNAE). Extensível. */
    private static final Map<String, Cnae> TABELA = tabela();

    private final Optional<TradutorCnaeLlm> tradutor;
    private final Map<String, List<Cnae>> cache = new ConcurrentHashMap<>();

    public ResolvedorCnaeImpl(Optional<TradutorCnaeLlm> tradutor) {
        this.tradutor = tradutor;
    }

    @Override
    public List<Cnae> resolver(String termo) {
        if (termo == null || termo.isBlank()) {
            return List.of();
        }
        String chave = normalizar(termo);

        Cnae exato = TABELA.get(chave);
        if (exato != null) {
            return List.of(exato);
        }
        // Cobre plurais e variações ("barbearias" contém "barbearia").
        for (Map.Entry<String, Cnae> e : TABELA.entrySet()) {
            if (chave.contains(e.getKey())) {
                return List.of(e.getValue());
            }
        }
        // Fallback LLM (com cache); sem tradutor configurado, devolve vazio.
        return cache.computeIfAbsent(chave,
                k -> tradutor.map(t -> t.traduzir(termo)).orElseGet(List::of));
    }

    private static String normalizar(String s) {
        String n = Normalizer.normalize(s.trim().toLowerCase(), Normalizer.Form.NFD);
        return n.replaceAll("\\p{M}+", "");
    }

    private static Map<String, Cnae> tabela() {
        Map<String, Cnae> m = new LinkedHashMap<>();
        m.put("barbearia", new Cnae("9602-5/01", "Cabeleireiros, manicure e pedicure"));
        m.put("salao de beleza", new Cnae("9602-5/02", "Atividades de estética e outros serviços de cuidados com a beleza"));
        m.put("restaurante", new Cnae("5611-2/01", "Restaurantes e similares"));
        m.put("pizzaria", new Cnae("5611-2/01", "Restaurantes e similares"));
        m.put("lanchonete", new Cnae("5611-2/03", "Lanchonetes, casas de chá, de sucos e similares"));
        m.put("hamburgueria", new Cnae("5611-2/03", "Lanchonetes, casas de chá, de sucos e similares"));
        m.put("cafeteria", new Cnae("5611-2/03", "Lanchonetes, casas de chá, de sucos e similares"));
        m.put("padaria", new Cnae("4721-1/02", "Padaria e confeitaria com predominância de revenda"));
        m.put("academia", new Cnae("9313-1/00", "Atividades de condicionamento físico"));
        m.put("pet shop", new Cnae("4789-0/04", "Comércio varejista de animais vivos e de artigos e alimentos para animais"));
        m.put("farmacia", new Cnae("4771-7/01", "Comércio varejista de produtos farmacêuticos, sem manipulação de fórmulas"));
        m.put("supermercado", new Cnae("4711-3/02", "Comércio varejista de mercadorias em geral (supermercados)"));
        m.put("mercado", new Cnae("4712-1/00", "Comércio varejista de mercadorias em geral (minimercados, mercearias)"));
        m.put("oficina mecanica", new Cnae("4520-0/01", "Serviços de manutenção e reparação mecânica de veículos automotores"));
        m.put("dentista", new Cnae("8630-5/04", "Atividade odontológica"));
        m.put("clinica odontologica", new Cnae("8630-5/04", "Atividade odontológica"));
        m.put("advocacia", new Cnae("6911-7/01", "Serviços advocatícios"));
        m.put("contabilidade", new Cnae("6920-6/01", "Atividades de contabilidade"));
        return m;
    }
}
