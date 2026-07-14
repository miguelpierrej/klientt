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
        job.setCnae(request.cnae());
        job.setEstado(EstadoJob.A_PROCESSAR);
        return jobRepository.save(job).getId();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<JobBusca> obter(Long jobId) {
        return jobRepository.findById(jobId);
    }

    @Override
    @Transactional
    public void registarResultado(Long jobId, Long empresaId) {
        // save() faz merge (chave atribuída): reenvios atualizam em vez de duplicar.
        resultadoRepository.save(new JobResultado(jobId, empresaId));
    }

    @Override
    @Transactional
    public void registarCursor(Long jobId, String cursor) {
        jobRepository.findById(jobId).ifPresent(job -> job.setCursor(cursor));
    }

    @Override
    @Transactional
    public void concluir(Long jobId) {
        jobRepository.findById(jobId).ifPresent(this::concluirJob);
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
