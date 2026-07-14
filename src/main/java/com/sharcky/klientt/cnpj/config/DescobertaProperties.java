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

    public boolean usaCasaDosDados() {
        return "casadosdados".equalsIgnoreCase(fonte);
    }
}
