package com.sharcky.klientt.busca.scoring;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.Sinais;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class AvaliadorLeadImplTest {

    private final AvaliadorLead avaliador = new AvaliadorLeadImpl();

    @Test
    void semSiteNotaBaixaPoucosSeguidores() {
        // sem site (+30) + nota<4 (+15) + <500 seguidores (+15) = 60
        Empresa e = empresa(new BigDecimal("3.4"), false, null, false, 180);
        assertThat(avaliador.avaliar(e).score()).isEqualTo(60);
    }

    @Test
    void piorCenarioComProcon() {
        // sem site (+30) + nota<4 (+15) + <500 (+15) + procon (+25) = 85
        Empresa e = empresa(new BigDecimal("2.8"), false, null, true, 0);
        AvaliacaoLead a = avaliador.avaliar(e);
        assertThat(a.score()).isEqualTo(85);
        assertThat(a.proconEviteSite()).isTrue();
        assertThat(a.temSite()).isFalse();
    }

    @Test
    void empresaSaudavelPontuaZero() {
        // tem site rápido (+0), nota alta (+0), muitos seguidores (+0) = 0
        Empresa e = empresa(new BigDecimal("4.6"), true, 900, false, 5200);
        assertThat(avaliador.avaliar(e).score()).isZero();
    }

    @Test
    void siteLentoPontua() {
        // tem site mas lento >3000ms (+20); nota>=4 e >=500 seguidores não somam
        Empresa e = empresa(new BigDecimal("4.1"), true, 4200, false, 820);
        AvaliacaoLead a = avaliador.avaliar(e);
        assertThat(a.score()).isEqualTo(20);
        assertThat(a.siteLento()).isTrue();
    }

    @Test
    void semSinaisNaoRebenta() {
        Empresa e = new Empresa();
        e.setNome("Sem sinais");
        AvaliacaoLead a = avaliador.avaliar(e);
        // sem site (+30) + nota 0 (<4, +15) + 0 seguidores (+15) = 60
        assertThat(a.score()).isEqualTo(60);
        assertThat(a.notaGoogle()).isZero();
    }

    private Empresa empresa(BigDecimal nota, boolean temSite, Integer velocidadeMs,
                            boolean procon, int seguidores) {
        Empresa e = new Empresa();
        e.setNome("Teste");
        e.setCidade("Lisboa");

        Sinais s = new Sinais();
        s.setNotaGoogle(nota);
        s.setSiteExiste(temSite);
        s.setSiteVelocidadeMs(velocidadeMs);
        s.setProconEviteSite(procon);
        e.definirSinais(s);

        EmpresaRede r = new EmpresaRede();
        r.setRede("instagram");
        r.setSeguidores(seguidores);
        e.adicionarRede(r);

        return e;
    }
}
