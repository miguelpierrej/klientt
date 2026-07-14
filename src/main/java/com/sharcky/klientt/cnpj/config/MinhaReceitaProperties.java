package com.sharcky.klientt.cnpj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Fonte de descoberta <b>gratuita</b>: Minha Receita (https://minhareceita.org), dados públicos da
 * Receita Federal. Busca por CNAE + UF/município (nome), com paginação por cursor. Sem chave/custo.
 * Ligada por default — é a fonte de descoberta primária (ver 'Nova integração.md').
 */
@Component
@ConfigurationProperties(prefix = "klientt.minha-receita")
@Getter
@Setter
public class MinhaReceitaProperties {

    private boolean enabled = true;

    private String baseUrl = "https://minhareceita.org";

    /**
     * Itens por página ao paginar por cursor. Pequeno de propósito: a instância pública fica lenta
     * (timeout) em lotes maiores (~>10). Paginamos por cursor até atingir o limite. Sobe se
     * auto-hospedares uma instância rápida.
     */
    private int tamanhoPagina = 10;

    /** Teto de páginas percorridas por busca (limita o custo do filtro client-side por cidade). */
    private int maxPaginas = 40;
}
