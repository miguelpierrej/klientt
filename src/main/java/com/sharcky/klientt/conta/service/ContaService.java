package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.ResumoConta;

/**
 * Resumo da conta do utilizador (plano + consumo).
 */
public interface ContaService {

    ResumoConta resumo(Long utilizadorId);
}
