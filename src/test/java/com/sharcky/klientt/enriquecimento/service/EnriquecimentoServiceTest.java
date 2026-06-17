package com.sharcky.klientt.enriquecimento.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoCallback;
import com.sharcky.klientt.scraper.dto.SinaisPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnriquecimentoServiceTest {

    @Mock EmpresaRepository empresaRepository;
    @Mock ScrapeMapper scrapeMapper;
    @Mock JobService jobService;
    @InjectMocks EnriquecimentoService service;

    @Test
    void aplicaSinaisEnderecoMapsERegistaEnriquecimento() {
        Empresa empresa = new Empresa();
        empresa.setCnpj("12345678000199");
        empresa.setEndereco("RUA EXEMPLO, 10, CENTRO");
        when(empresaRepository.findFirstByCnpj("12345678000199")).thenReturn(Optional.of(empresa));
        when(scrapeMapper.toSinais(any())).thenReturn(new Sinais());
        when(empresaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        SinaisPayload sinais = new SinaisPayload(new BigDecimal("4.3"), 80, true, 1200, true, 5, null, false);
        EnriquecimentoCallback cb = new EnriquecimentoCallback(
                "7", "12.345.678/0001-99", true, "RUA EXEMPLO 10 CENTRO SAO PAULO", sinais, List.of());

        service.aplicar(cb);

        assertThat(empresa.getSinais()).isNotNull();
        assertThat(empresa.getEnderecoMaps()).isEqualTo("RUA EXEMPLO 10 CENTRO SAO PAULO");
        assertThat(empresa.getEnderecoDivergente()).isFalse();   // partilha "rua/exemplo/centro" → não divergente
        verify(jobService).registarEnriquecimento(7L);
    }

    @Test
    void semEmpresaAindaRegistaEnriquecimento() {
        when(empresaRepository.findFirstByCnpj("12345678000199")).thenReturn(Optional.empty());

        service.aplicar(new EnriquecimentoCallback("7", "12345678000199", false, null, null, null));

        verify(empresaRepository, never()).save(any());
        verify(jobService).registarEnriquecimento(7L);   // job não fica preso
    }

    @Test
    void divergenciaDeEndereco() {
        assertThat(EnriquecimentoService.divergente("Rua A, 10, Centro", "Rua A 10 Centro SP")).isFalse();
        assertThat(EnriquecimentoService.divergente("Rua Alfa, Centro", "Avenida Beta, Jardins")).isTrue();
        assertThat(EnriquecimentoService.divergente(null, "Rua A")).isNull();
        assertThat(EnriquecimentoService.divergente("Rua A", null)).isNull();
    }
}
