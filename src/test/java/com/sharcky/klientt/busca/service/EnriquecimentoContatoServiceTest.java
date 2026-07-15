package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.cnpj.FonteContatoCnpj;
import com.sharcky.klientt.cnpj.config.ContatoFallbackProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnriquecimentoContatoServiceTest {

    @Mock ContatoFallbackProperties contatoFallback;
    @Mock FonteContatoCnpj fonteContato;
    @Mock EmpresaCacheService cacheService;
    @InjectMocks EnriquecimentoContatoService service;

    private final List<EmpresaPayload> semContato = List.of(new EmpresaPayload(
            "Barbearia X", "12345678000199", null, null, null, "São Paulo", null, null, null,
            null, List.of(), List.of(), List.of()));

    @Test
    void ligadoPreencheContatoEmFalta() {
        when(contatoFallback.isEnabled()).thenReturn(true);
        when(fonteContato.consultar("12345678000199"))
                .thenReturn(new FonteContatoCnpj.Contatos(List.of("11-5555-5555"), List.of()));

        service.enriquecer(semContato);

        verify(fonteContato).consultar("12345678000199");
        verify(cacheService).upsert(any());   // funde o contacto encontrado
    }

    @Test
    void desligadoNaoConsulta() {
        when(contatoFallback.isEnabled()).thenReturn(false);

        service.enriquecer(semContato);

        verify(fonteContato, never()).consultar(any());
        verify(cacheService, never()).upsert(any());
    }

    @Test
    void naoConsultaQuandoJaTemContato() {
        List<EmpresaPayload> comContato = List.of(new EmpresaPayload(
                "Barbearia Y", "98765432000111", "11-4444-4444", null, null, "São Paulo", null, null, null,
                null, List.of("11-4444-4444"), List.of(), List.of()));
        when(contatoFallback.isEnabled()).thenReturn(true);

        service.enriquecer(comContato);

        verify(fonteContato, never()).consultar(any());   // já tem contacto → não gasta a consulta
    }

    @Test
    void umaFalhaNaoAbortaOsRestantes() {
        List<EmpresaPayload> dois = List.of(
                new EmpresaPayload("A", "11111111000191", null, null, null, "SP", null, null, null,
                        null, List.of(), List.of(), List.of()),
                new EmpresaPayload("B", "22222222000191", null, null, null, "SP", null, null, null,
                        null, List.of(), List.of(), List.of()));
        when(contatoFallback.isEnabled()).thenReturn(true);
        when(fonteContato.consultar("11111111000191")).thenThrow(new RuntimeException("timeout"));
        when(fonteContato.consultar("22222222000191"))
                .thenReturn(new FonteContatoCnpj.Contatos(List.of("11-9999-9999"), List.of()));

        service.enriquecer(dois);

        verify(fonteContato).consultar("22222222000191");   // continuou apesar da falha do 1º
        verify(cacheService).upsert(any());
    }
}
