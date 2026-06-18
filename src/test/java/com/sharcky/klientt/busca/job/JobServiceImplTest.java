package com.sharcky.klientt.busca.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock JobBuscaRepository jobRepository;
    @Mock JobResultadoRepository resultadoRepository;
    @InjectMocks JobServiceImpl jobService;

    @Test
    void concluirMarcaConcluidoEDataDeConclusao() {
        JobBusca job = new JobBusca();
        job.setEstado(EstadoJob.A_PROCESSAR);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        jobService.concluir(5L);

        assertThat(job.getEstado()).isEqualTo(EstadoJob.CONCLUIDO);
        assertThat(job.getConcluidoEm()).isNotNull();
    }

    @Test
    void marcarErroMarcaErro() {
        JobBusca job = new JobBusca();
        job.setEstado(EstadoJob.A_PROCESSAR);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        jobService.marcarErro(5L);

        assertThat(job.getEstado()).isEqualTo(EstadoJob.ERRO);
    }

    @Test
    void registarResultadoGuardaLigacaoJobEmpresa() {
        jobService.registarResultado(5L, 100L);

        ArgumentCaptor<JobResultado> captor = ArgumentCaptor.forClass(JobResultado.class);
        verify(resultadoRepository).save(captor.capture());
        assertThat(captor.getValue().getJobId()).isEqualTo(5L);
        assertThat(captor.getValue().getEmpresaId()).isEqualTo(100L);
    }
}
