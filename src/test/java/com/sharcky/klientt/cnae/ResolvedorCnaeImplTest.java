package com.sharcky.klientt.cnae;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ResolvedorCnaeImplTest {

    private CnaeCatalogo cnae(String codigo, String descricao) {
        CnaeCatalogo c = new CnaeCatalogo();
        c.setCodigo(codigo);
        c.setDescricao(descricao);
        return c;
    }

    private CnaeCatalogoRepository catalogo() {
        CnaeCatalogoRepository repo = mock(CnaeCatalogoRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                cnae("9602501", "CABELEIREIROS, MANICURE E PEDICURE"),
                cnae("5611201", "RESTAURANTES E SIMILARES"),
                cnae("4711302", "COMÉRCIO VAREJISTA DE MERCADORIAS EM GERAL (SUPERMERCADOS)")));
        return repo;
    }

    @Test
    void sinonimoColoquialValidadoNoCatalogo() {
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), catalogo());

        List<Cnae> res = r.resolver("barbearias em São Paulo");

        assertThat(res).singleElement().satisfies(c -> {
            assertThat(c.codigo()).isEqualTo("9602501");
            assertThat(c.descricao()).isEqualTo("CABELEIREIROS, MANICURE E PEDICURE");   // descrição oficial
        });
    }

    @Test
    void buscaPorDescricaoNoCatalogo() {
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), catalogo());

        List<Cnae> res = r.resolver("restaurante");

        assertThat(res).singleElement()
                .satisfies(c -> assertThat(c.codigo()).isEqualTo("5611201"));
    }

    @Test
    void fallbackLlmComCodigoValidoUsaDescricaoOficial() {
        TradutorCnaeLlm tradutor = mock(TradutorCnaeLlm.class);
        when(tradutor.traduzir("estúdio de tatuagem"))
                .thenReturn(List.of(new Cnae("9602501", "descrição do LLM (ignorada)")));
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.of(tradutor), catalogo());

        List<Cnae> res = r.resolver("estúdio de tatuagem");

        assertThat(res).singleElement().satisfies(c -> {
            assertThat(c.codigo()).isEqualTo("9602501");
            assertThat(c.descricao()).isEqualTo("CABELEIREIROS, MANICURE E PEDICURE");   // do catálogo, não do LLM
        });
    }

    @Test
    void fallbackLlmComCodigoInexistenteEhDescartado() {
        TradutorCnaeLlm tradutor = mock(TradutorCnaeLlm.class);
        when(tradutor.traduzir("coisa exótica")).thenReturn(List.of(new Cnae("0000000", "inventado")));
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.of(tradutor), catalogo());

        assertThat(r.resolver("coisa exótica")).isEmpty();   // código não existe no catálogo
    }

    @Test
    void semFallbackEForaDoCatalogoDevolveVazio() {
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), catalogo());

        assertThat(r.resolver("xyzqualquercoisa")).isEmpty();
    }

    @Test
    void termoVazioDevolveVazio() {
        // Não toca no catálogo — sem stub de findAll.
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), mock(CnaeCatalogoRepository.class));

        assertThat(r.resolver("  ")).isEmpty();
    }

    @Test
    void candidatosDevolveVariosParaTermoAmbiguo() {
        CnaeCatalogoRepository repo = mock(CnaeCatalogoRepository.class);
        when(repo.findAll()).thenReturn(List.of(
                cnae("9521500", "REPARAÇÃO E MANUTENÇÃO DE EQUIPAMENTOS ELETRODOMÉSTICOS"),
                cnae("9511800", "REPARAÇÃO E MANUTENÇÃO DE COMPUTADORES E PERIFÉRICOS"),
                cnae("9512600", "REPARAÇÃO E MANUTENÇÃO DE EQUIPAMENTOS DE COMUNICAÇÃO")));
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), repo);

        List<Cnae> res = r.candidatos("reparação e manutenção");

        assertThat(res).hasSize(3).extracting(Cnae::codigo)
                .containsExactlyInAnyOrder("9521500", "9511800", "9512600");
    }

    @Test
    void candidatosUsaSinonimoNoTopo() {
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), catalogo());

        List<Cnae> res = r.candidatos("barbearia");

        assertThat(res).isNotEmpty();
        assertThat(res.get(0).codigo()).isEqualTo("9602501");   // sinónimo no topo
    }

    @Test
    void candidatosTermoVazioDevolveVazio() {
        ResolvedorCnae r = new ResolvedorCnaeImpl(Optional.empty(), mock(CnaeCatalogoRepository.class));

        assertThat(r.candidatos("  ")).isEmpty();
    }
}
