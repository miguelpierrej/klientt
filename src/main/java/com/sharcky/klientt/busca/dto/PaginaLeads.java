package com.sharcky.klientt.busca.dto;

import java.util.List;

/**
 * Uma página de leads para a lista de resultados (paginação server-side via HTMX).
 * {@code pagina} é 1-based.
 */
public record PaginaLeads(
        List<LeadResponse> leads,
        int pagina,
        int totalPaginas,
        long total,
        int tamanho
) {

    /** Fatia a página {@code pagina} (1-based) de {@code todos}, com {@code tamanho} por página. */
    public static PaginaLeads de(List<LeadResponse> todos, int pagina, int tamanho) {
        int total = todos.size();
        int totalPaginas = Math.max(1, (int) Math.ceil((double) total / tamanho));
        int p = Math.min(Math.max(1, pagina), totalPaginas);
        int inicio = (p - 1) * tamanho;
        int fim = Math.min(inicio + tamanho, total);
        List<LeadResponse> fatia = inicio >= total ? List.of() : todos.subList(inicio, fim);
        return new PaginaLeads(fatia, p, totalPaginas, total, tamanho);
    }

    public boolean temAnterior() {
        return pagina > 1;
    }

    public boolean temSeguinte() {
        return pagina < totalPaginas;
    }

    public int anterior() {
        return pagina - 1;
    }

    public int seguinte() {
        return pagina + 1;
    }
}
