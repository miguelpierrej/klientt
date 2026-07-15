package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.perfil.PerfilCliente;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RelevanciaServiceTest {

    private final RelevanciaService service = new RelevanciaService();

    private PerfilCliente perfilCompleto() {
        PerfilCliente p = new PerfilCliente();
        p.setRegioesAlvo("Bauru/SP");
        p.setPortesAlvo("MICRO");
        p.setQuerComContato(true);
        p.setQuerSemSite(true);
        p.setQuerSimplesMei(false);
        return p;   // possíveis: 30 (região) + 25 (porte) + 20 (contato) + 15 (sem site) = 90
    }

    private Empresa empresa(String cidade, String porte, String website, boolean comContato) {
        Empresa e = new Empresa();
        e.setCidade(cidade);
        e.setPorte(porte);
        e.setWebsite(website);
        if (comContato) {
            Contato c = new Contato();
            c.setTipo("telefone");
            c.setValor("14-3232-1010");
            e.adicionarContato(c);
        }
        return e;
    }

    @Test
    void otimoFitQuandoTudoBate() {
        Empresa e = empresa("Bauru", "MICRO EMPRESA", null, true);   // sem site, tem contato
        RelevanciaService.Fit fit = service.avaliar(perfilCompleto(), e);
        assertThat(fit.pontos()).isEqualTo(90);
        assertThat(fit.possiveis()).isEqualTo(90);
        assertThat(fit.rotulo()).isEqualTo("Ótimo fit");
    }

    @Test
    void bomFitParcial() {
        // Bate região + contato (50/90 ≈ 0.55) — mas não porte (MÉDIA) nem sem-site (tem site).
        Empresa e = empresa("Bauru", "DEMAIS", "site.com", true);
        RelevanciaService.Fit fit = service.avaliar(perfilCompleto(), e);
        assertThat(fit.pontos()).isEqualTo(50);
        assertThat(fit.rotulo()).isEqualTo("Bom fit");
    }

    @Test
    void semRotuloQuandoQuaseNadaBate() {
        // Só contato (20/90 ≈ 0.22) → abaixo do limiar.
        Empresa e = empresa("São Paulo", "DEMAIS", "site.com", true);
        assertThat(service.avaliar(perfilCompleto(), e).rotulo()).isNull();
    }

    @Test
    void acentoECaixaNaoImportamNaRegiao() {
        PerfilCliente p = new PerfilCliente();
        p.setRegioesAlvo("São Paulo/SP");
        p.setQuerComContato(false);
        Empresa e = empresa("sao paulo", "DEMAIS", "s", false);
        assertThat(service.avaliar(p, e).pontos()).isEqualTo(30);   // região bate (30/30)
    }

    @Test
    void tokenPorteMapeiaOsRotulosDaReceita() {
        assertThat(RelevanciaService.tokenPorte("MICRO EMPRESA")).isEqualTo("MICRO");
        assertThat(RelevanciaService.tokenPorte("MEI")).isEqualTo("MEI");
        assertThat(RelevanciaService.tokenPorte("EMPRESA DE PEQUENO PORTE")).isEqualTo("PEQUENA");
        assertThat(RelevanciaService.tokenPorte("DEMAIS")).isEqualTo("GRANDE");
        assertThat(RelevanciaService.tokenPorte(null)).isNull();
    }
}
