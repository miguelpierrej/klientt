package com.sharcky.klientt.enriquecimento;

import com.sharcky.klientt.busca.job.JobBusca;
import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.enriquecimento.dto.EnrichCallback;
import com.sharcky.klientt.enriquecimento.dto.EnrichCallback.EmpresaEnriquecida;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EnriquecimentoServiceImplTest {

    @Mock EmpresaCacheService cacheService;
    @Mock JobService jobService;

    private EnriquecimentoServiceImpl service() {
        return new EnriquecimentoServiceImpl(cacheService, jobService);
    }

    private static EmpresaEnriquecida empresa() {
        return new EmpresaEnriquecida(
                "12345678000199", "Acme LDA", "Acme", "https://acme.com.br",
                List.of(new EnrichCallback.Email("contato@acme.com.br", "site", 100)),
                List.of(new EnrichCallback.Telefone("11988887777", "celular", "site", 100)),
                List.of(new EnrichCallback.Rede("instagram", "https://instagram.com/acme")),
                new EnrichCallback.Endereco("Rua A", "10", "Centro", "São Paulo", "SP", "01000-000"),
                new EnrichCallback.GoogleMaps("https://maps.google/x", 4.7, 233, "Barbearia", null),
                new EnrichCallback.Cadastrais("Cabeleireiros", "ATIVA", "ME", null, "2015-03-01", "LTDA"));
    }

    @Test
    void fundeEmpresaEnriquecidaEConcluiNoEstadoTerminal() {
        Empresa persistida = new Empresa();
        persistida.setId(99L);
        when(jobService.obter(7L)).thenReturn(Optional.of(new JobBusca()));
        when(cacheService.upsert(any())).thenReturn(persistida);

        service().aplicar(new EnrichCallback("7", "CONCLUIDO", null, List.of(empresa())));

        ArgumentCaptor<Empresa> captor = ArgumentCaptor.forClass(Empresa.class);
        verify(cacheService).upsert(captor.capture());
        Empresa e = captor.getValue();
        assertThat(e.getCnpj()).isEqualTo("12345678000199");
        assertThat(e.getNota()).isEqualTo(4.7);
        assertThat(e.getAvaliacoes()).isEqualTo(233);
        assertThat(e.getContatos()).extracting("tipo", "valor")
                .containsExactlyInAnyOrder(tuple("email", "contato@acme.com.br"),
                        tuple("telefone", "11988887777"));
        assertThat(e.getRedes()).singleElement()
                .extracting("rede", "url").containsExactly("instagram", "https://instagram.com/acme");
        assertThat(e.getDataAbertura()).isEqualTo(java.time.LocalDate.of(2015, 3, 1));

        verify(jobService).registarResultado(7L, 99L);
        verify(jobService).concluir(7L);
    }

    @Test
    void desserializaOJsonRealDoScraper() throws Exception {
        // Payload no formato exato que o scraper emite (ver 'Novo Fluxo.md' / scraper/pipeline.py).
        String json = """
            {"buscaId":"7","estado":"CONCLUIDO","erro":null,"empresas":[{
              "cnpj":"12345678000199","razaoSocial":"Acme LDA","nomeFantasia":"Acme",
              "emails":[{"email":"contato@acme.com.br","fonte":"site","confianca":100}],
              "telefones":[{"telefone":"11988887777","tipo":"celular","fonte":"site","confianca":100}],
              "website":"https://acme.com.br",
              "redes":[{"rede":"instagram","url":"https://instagram.com/acme"}],
              "endereco":{"logradouro":"Rua A","numero":"10","bairro":"Centro","cidade":"São Paulo","uf":"SP","cep":"01000-000"},
              "googleMaps":{"url":"https://maps/x","nota":4.7,"avaliacoes":233,"categoria":"Barbearia","horario":null},
              "dadosCadastrais":{"cnae":"Cabeleireiros","situacaoCadastral":"ATIVA","porte":"ME","capitalSocial":1000.00,"dataAbertura":"2015-03-01","naturezaJuridica":"LTDA"},
              "fontesConsultadas":["site","maps"],"erro":null,
              "estatisticas":{"tempoProcessamentoMs":2060,"requisicoes":9,"playwrightUtilizado":false,"cacheHits":0}
            }]}""";

        EnrichCallback cb = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, EnrichCallback.class);

        assertThat(cb.estado()).isEqualTo("CONCLUIDO");
        assertThat(cb.empresas()).hasSize(1);
        EmpresaEnriquecida e = cb.empresas().get(0);
        assertThat(e.cnpj()).isEqualTo("12345678000199");
        assertThat(e.emails()).singleElement().extracting(EnrichCallback.Email::email).isEqualTo("contato@acme.com.br");
        assertThat(e.telefones().get(0).tipo()).isEqualTo("celular");
        assertThat(e.googleMaps().nota()).isEqualTo(4.7);
        assertThat(e.googleMaps().avaliacoes()).isEqualTo(233);
        assertThat(e.dadosCadastrais().capitalSocial()).isEqualByComparingTo("1000.00");
        assertThat(e.redes()).singleElement().extracting(EnrichCallback.Rede::rede).isEqualTo("instagram");
    }

    @Test
    void loteParcialNaoConcluiJob() {
        when(jobService.obter(7L)).thenReturn(Optional.of(new JobBusca()));
        when(cacheService.upsert(any())).thenReturn(persistidaComId());

        service().aplicar(new EnrichCallback("7", "PARCIAL", null, List.of(empresa())));

        verify(jobService).registarResultado(7L, 99L);
        verify(jobService, never()).concluir(any());
    }

    @Test
    void callbackDeJobInexistenteEIgnorado() {
        // obter(1) devolve Optional.empty() (default) → job não existe (ex.: app reiniciado).
        service().aplicar(new EnrichCallback("1", "CONCLUIDO", null, List.of(empresa())));

        verify(cacheService, never()).upsert(any());
        verify(jobService, never()).registarResultado(any(), any());
        verify(jobService, never()).concluir(any());
    }

    private static Empresa persistidaComId() {
        Empresa e = new Empresa();
        e.setId(99L);
        return e;
    }

    // import estático de tuple sem poluir o topo
    private static org.assertj.core.groups.Tuple tuple(Object... v) {
        return org.assertj.core.groups.Tuple.tuple(v);
    }
}
