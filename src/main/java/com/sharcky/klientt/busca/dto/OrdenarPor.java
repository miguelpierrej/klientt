package com.sharcky.klientt.busca.dto;

/** Critérios de ordenação da lista de leads. */
public enum OrdenarPor {
    SCORE,        // maior score primeiro (default)
    NOTA,         // menor nota Google primeiro (mais "dor")
    SEGUIDORES,   // menos seguidores primeiro
    NOME          // alfabético
}
