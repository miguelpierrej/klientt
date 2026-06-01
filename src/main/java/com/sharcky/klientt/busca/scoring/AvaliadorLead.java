package com.sharcky.klientt.busca.scoring;

import com.sharcky.klientt.empresa.model.Empresa;

/**
 * Deriva os sinais de uma empresa e calcula o score de oportunidade (ARQUITETURA §6).
 */
public interface AvaliadorLead {

    AvaliacaoLead avaliar(Empresa empresa);
}
