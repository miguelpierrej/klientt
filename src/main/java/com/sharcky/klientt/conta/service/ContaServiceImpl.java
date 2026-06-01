package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.ResumoConta;
import com.sharcky.klientt.conta.model.Plano;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaServiceImpl implements ContaService {

    private final UtilizadorRepository utilizadorRepository;
    private final QuotaService quotaService;

    public ContaServiceImpl(UtilizadorRepository utilizadorRepository, QuotaService quotaService) {
        this.utilizadorRepository = utilizadorRepository;
        this.quotaService = quotaService;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoConta resumo(Long utilizadorId) {
        Utilizador u = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + utilizadorId));

        Plano plano = u.getPlano();
        int limite = plano != null ? plano.getLimiteLeadsMes() : 0;
        long consumo = quotaService.consumoMesAtual(utilizadorId);
        long restante = Math.max(0, limite - consumo);

        return new ResumoConta(
                u.getNome(),
                u.getEmail(),
                plano != null ? plano.getNome() : "—",
                limite,
                consumo,
                restante);
    }
}
