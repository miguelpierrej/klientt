package com.sharcky.klientt.busca.job;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JobServiceImplTest {

    @Mock JobBuscaRepository jobRepository;
    @Mock JobResultadoRepository resultadoRepository;
    @InjectMocks JobServiceImpl jobService;

    @Test
    void jobConcluiSoQuandoAsDuasFontesReportam() {
        JobBusca job = new JobBusca();
        job.setEstado(EstadoJob.A_PROCESSAR);
        job.setFontesEsperadas(2);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        jobService.marcarFonteConcluida(5L);   // 1ª fonte (ex.: scraper)
        assertThat(job.getEstado()).isEqualTo(EstadoJob.A_PROCESSAR);

        jobService.marcarFonteConcluida(5L);   // 2ª fonte (ex.: CNPJ)
        assertThat(job.getEstado()).isEqualTo(EstadoJob.CONCLUIDO);
        assertThat(job.getConcluidoEm()).isNotNull();
    }

    @Test
    void fonteAReportarAposErroNaoReabreOJob() {
        JobBusca job = new JobBusca();
        job.setEstado(EstadoJob.ERRO);
        job.setFontesEsperadas(2);
        when(jobRepository.findById(5L)).thenReturn(Optional.of(job));

        jobService.marcarFonteConcluida(5L);

        assertThat(job.getEstado()).isEqualTo(EstadoJob.ERRO);
        assertThat(job.getFontesConcluidas()).isZero();
    }
}
