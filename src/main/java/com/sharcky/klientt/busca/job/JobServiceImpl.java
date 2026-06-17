package com.sharcky.klientt.busca.job;

import com.sharcky.klientt.busca.dto.BuscaRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class JobServiceImpl implements JobService {

    private final JobBuscaRepository jobRepository;
    private final JobResultadoRepository resultadoRepository;

    public JobServiceImpl(JobBuscaRepository jobRepository, JobResultadoRepository resultadoRepository) {
        this.jobRepository = jobRepository;
        this.resultadoRepository = resultadoRepository;
    }

    @Override
    @Transactional
    public Long criar(BuscaRequest request, Long utilizadorId) {
        JobBusca job = new JobBusca();
        job.setUtilizadorId(utilizadorId);
        job.setTipo(request.tipo());
        job.setTermo(request.termo());
        job.setRegiao(request.regiao());
        job.setEstado(EstadoJob.A_PROCESSAR);
        // Fonte primária única (Casa dos Dados); o enriquecimento Maps (Fase 2) será contado à parte.
        job.setFontesEsperadas(1);
        return jobRepository.save(job).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobBusca> obter(Long jobId) {
        return jobRepository.findById(jobId);
    }

    @Override
    @Transactional
    public void registarResultado(Long jobId, Long empresaId, int score) {
        // save() faz merge (chave atribuída): callbacks repetidos atualizam em vez de duplicar.
        resultadoRepository.save(new JobResultado(jobId, empresaId, score));
    }

    @Override
    @Transactional
    public void concluir(Long jobId) {
        jobRepository.findById(jobId).ifPresent(this::concluirJob);
    }

    @Override
    @Transactional
    public void marcarFonteConcluida(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            // Só conta enquanto o job está a processar (ignora fontes a reportar após ERRO/CONCLUIDO).
            if (job.getEstado() != EstadoJob.A_PROCESSAR) {
                return;
            }
            job.setFontesConcluidas(job.getFontesConcluidas() + 1);
            if (job.getFontesConcluidas() >= job.getFontesEsperadas()) {
                concluirJob(job);
            }
        });
    }

    private void concluirJob(JobBusca job) {
        job.setEstado(EstadoJob.CONCLUIDO);
        job.setConcluidoEm(LocalDateTime.now());
    }

    @Override
    @Transactional
    public void marcarErro(Long jobId) {
        jobRepository.findById(jobId).ifPresent(job -> {
            job.setEstado(EstadoJob.ERRO);
            job.setConcluidoEm(LocalDateTime.now());
        });
    }
}
