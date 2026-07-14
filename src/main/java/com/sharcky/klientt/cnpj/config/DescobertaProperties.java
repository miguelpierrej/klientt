package com.sharcky.klientt.cnpj.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Seleção da fonte de descoberta por CNAE (ver 'Nova integração.md', D5).
 * Default {@code minhareceita} (grátis); {@code casadosdados} usa a fonte paga.
 * A busca por NOME vai sempre para a Casa dos Dados (o Minha Receita não a suporta).
 */
@Component
@ConfigurationProperties(prefix = "klientt.descoberta")
@Getter
@Setter
public class DescobertaProperties {

    /** {@code minhareceita} | {@code casadosdados}. */
    private String fonte = "minhareceita";

    /**
     * Fallback: quando a Minha Receita (grátis) <b>falha</b> (fora do ar/erro HTTP) — e NÃO quando
     * simplesmente não tem resultados — tenta a Casa dos Dados (paga). Só na página inicial.
     * Desliga com {@code false}.
     */
    private boolean fallbackCasadosdados = true;

    public boolean usaCasaDosDados() {
        return "casadosdados".equalsIgnoreCase(fonte);
    }

    /** Fallback ativo só faz sentido quando a fonte primária NÃO é já a Casa dos Dados. */
    public boolean fallbackAtivo() {
        return fallbackCasadosdados && !usaCasaDosDados();
    }
}
