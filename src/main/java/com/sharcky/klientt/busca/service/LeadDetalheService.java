package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.LeadDetalhe;

/**
 * Detalhe completo de um lead (empresa) para a vista de detalhe.
 */
public interface LeadDetalheService {

    LeadDetalhe detalhe(Long empresaId);
}
