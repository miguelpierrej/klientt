package com.sharcky.klientt.enriquecimento.dto;

import com.sharcky.klientt.scraper.dto.RedePayload;
import com.sharcky.klientt.scraper.dto.SinaisPayload;

import java.util.List;

/**
 * Callback do enriquecimento Maps (scraper → Klientt) — atado ao {@code cnpj} pedido.
 * {@code encontrado=false} quando o Maps não achou a empresa (o job conta na mesma).
 */
public record EnriquecimentoCallback(
        String buscaId,
        String cnpj,
        boolean encontrado,
        String enderecoMaps,
        SinaisPayload sinais,
        List<RedePayload> redes
) {
}
