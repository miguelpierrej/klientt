package com.sharcky.klientt.cnpj;

import java.util.List;

/**
 * Fonte de fallback para contactos por CNPJ (PLANO-SO-API.md, Fase D): quando a descoberta não trouxe
 * telefone nem email, consulta-se uma API pública de CNPJ (ex.: BrasilAPI) para tentar preencher.
 *
 * <p>Falha graciosa: em erro/desligada devolve {@link Contatos#vazio()}.
 */
public interface FonteContatoCnpj {

    Contatos consultar(String cnpj);

    /** Contactos devolvidos pela consulta por CNPJ. */
    record Contatos(List<String> telefones, List<String> emails) {

        public static Contatos vazio() {
            return new Contatos(List.of(), List.of());
        }

        public boolean isVazio() {
            return telefones.isEmpty() && emails.isEmpty();
        }
    }
}
