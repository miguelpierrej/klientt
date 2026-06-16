package com.sharcky.klientt.cnae;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ResolvedorCnaeImplTest {

    @Test
    void resolvePelaTabelaSemChamarLlm() {
        TradutorCnaeLlm tradutor = mock(TradutorCnaeLlm.class);
        ResolvedorCnae resolvedor = new ResolvedorCnaeImpl(Optional.of(tradutor));

        List<Cnae> r = resolvedor.resolver("barbearias em São Paulo");

        assertThat(r).extracting(Cnae::codigo).containsExactly("9602-5/01");
        verifyNoInteractions(tradutor);
    }

    @Test
    void resolvePeloFallbackECacheia() {
        TradutorCnaeLlm tradutor = mock(TradutorCnaeLlm.class);
        when(tradutor.traduzir("estúdio de tatuagem"))
                .thenReturn(List.of(new Cnae("9609-2/06", "Serviços de tatuagem e colocação de piercing")));
        ResolvedorCnae resolvedor = new ResolvedorCnaeImpl(Optional.of(tradutor));

        List<Cnae> primeira = resolvedor.resolver("estúdio de tatuagem");
        List<Cnae> segunda = resolvedor.resolver("estúdio de tatuagem");   // deve vir da cache

        assertThat(primeira).extracting(Cnae::codigo).containsExactly("9609-2/06");
        assertThat(segunda).isEqualTo(primeira);
        verify(tradutor, times(1)).traduzir("estúdio de tatuagem");   // LLM chamado uma só vez
    }

    @Test
    void semFallbackEForaDaTabelaDevolveVazio() {
        ResolvedorCnae resolvedor = new ResolvedorCnaeImpl(Optional.empty());

        assertThat(resolvedor.resolver("coworking espacial")).isEmpty();
    }

    @Test
    void termoVazioDevolveVazio() {
        ResolvedorCnae resolvedor = new ResolvedorCnaeImpl(Optional.empty());

        assertThat(resolvedor.resolver("  ")).isEmpty();
    }
}
