package com.sharcky.klientt.busca.job;

/**
 * Estado de um job de busca (ARQUITETURA §5). Persistido como texto.
 */
public enum EstadoJob {
    PENDENTE,
    A_PROCESSAR,
    CONCLUIDO,
    ERRO
}
