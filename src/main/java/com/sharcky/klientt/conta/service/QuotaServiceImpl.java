package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.busca.job.JobResultadoRepository;
import com.sharcky.klientt.conta.model.Plano;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class QuotaServiceImpl implements QuotaService {

    private final UtilizadorRepository utilizadorRepository;
    private final JobResultadoRepository jobResultadoRepository;

    public QuotaServiceImpl(UtilizadorRepository utilizadorRepository,
                            JobResultadoRepository jobResultadoRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.jobResultadoRepository = jobResultadoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public void garantirDisponibilidade(Long utilizadorId) {
        Utilizador u = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + utilizadorId));

        int limite = limiteDoPlano(u.getPlano());
        if (consumoMesAtual(utilizadorId) >= limite) {
            throw new QuotaExcedidaException(limite);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public long consumoMesAtual(Long utilizadorId) {
        LocalDateTime inicioDoMes = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        return jobResultadoRepository.contarLeadsDoUtilizadorDesde(utilizadorId, inicioDoMes);
    }

    private int limiteDoPlano(Plano plano) {
        return plano != null ? plano.getLimiteLeadsMes() : 0;
    }
}
