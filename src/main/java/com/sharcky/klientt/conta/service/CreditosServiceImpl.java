package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreditosServiceImpl implements CreditosService {

    private final UtilizadorRepository utilizadorRepository;
    private final JobResultadoRepository jobResultadoRepository;
    /** Tamanho da 1ª página grátis (= página da UI). */
    private final int paginaGratis;

    public CreditosServiceImpl(UtilizadorRepository utilizadorRepository,
                               JobResultadoRepository jobResultadoRepository,
                               @Value("${klientt.busca.tamanho-pagina:20}") int paginaGratis) {
        this.utilizadorRepository = utilizadorRepository;
        this.jobResultadoRepository = jobResultadoRepository;
        this.paginaGratis = paginaGratis;
    }

    @Override
    @Transactional(readOnly = true)
    public long comprado(Long utilizadorId) {
        return utilizadorRepository.findById(utilizadorId).map(Utilizador::getCreditosLeads).orElse(0);
    }

    @Override
    @Transactional(readOnly = true)
    public long consumido(Long utilizadorId) {
        // Por job, os primeiros `paginaGratis` leads são grátis; o resto conta.
        return jobResultadoRepository.contarLeadsPorJobDoUtilizador(utilizadorId).stream()
                .mapToLong(n -> Math.max(0, n - paginaGratis))
                .sum();
    }

    @Override
    @Transactional(readOnly = true)
    public long disponivel(Long utilizadorId) {
        return Math.max(0, comprado(utilizadorId) - consumido(utilizadorId));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean temDisponivel(Long utilizadorId) {
        return disponivel(utilizadorId) > 0;
    }

    @Override
    @Transactional
    public void creditar(Long utilizadorId, int leads) {
        utilizadorRepository.findById(utilizadorId).ifPresent(u -> u.setCreditosLeads(u.getCreditosLeads() + leads));
    }
}
