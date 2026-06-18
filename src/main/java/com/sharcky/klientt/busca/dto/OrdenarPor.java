package com.sharcky.klientt.busca.dto;

/** Critérios de ordenação da lista de leads. */
public enum OrdenarPor {
    RELEVANCIA,   // contactáveis primeiro, depois mais recentes (default)
    RECENTE,      // empresas abertas mais recentemente primeiro
    NOME          // alfabético
}
