package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmpresaCacheServiceImplTest {

    @Mock EmpresaRepository repository;
    @InjectMocks EmpresaCacheServiceImpl cache;

    @Test
    void empresaNovaInsereComCnpjNormalizado() {
        when(repository.findFirstByCnpj("12345678000199")).thenReturn(Optional.empty());
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Barbearia do Zé", "São Paulo");
        fresca.setCnpj("12.345.678/0001-99");   // com máscara

        Empresa salva = cache.upsert(fresca);

        assertThat(salva.getCnpj()).isEqualTo("12345678000199");   // só dígitos
        verify(repository).save(fresca);
    }

    @Test
    void identidadePorCnpjFundeNaExistente() {
        Empresa existente = empresa("ZE SANTOS BARBEARIA LTDA", "São Paulo");
        existente.setCnpj("12345678000199");
        existente.setEmail("contato@ze.com.br");      // veio da Receita
        when(repository.findFirstByCnpj("12345678000199")).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Maps traz o mesmo CNPJ, sem email mas com website
        Empresa doMaps = empresa("Barbearia do Zé", "São Paulo");
        doMaps.setCnpj("12345678000199");
        doMaps.setWebsite("https://barbeariaze.com.br");

        Empresa fundida = cache.upsert(doMaps);

        assertThat(fundida).isSameAs(existente);
        assertThat(fundida.getEmail()).isEqualTo("contato@ze.com.br");          // não apagado
        assertThat(fundida.getWebsite()).isEqualTo("https://barbeariaze.com.br"); // preenchido
        verify(repository, never()).findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(any(), any());
    }

    @Test
    void semCnpjUsaFallbackNomeCidade() {
        Empresa existente = empresa("Barbearia do Zé", "São Paulo");
        existente.setTelefone("+5511999990000");
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase("Barbearia do Zé", "São Paulo"))
                .thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Barbearia do Zé", "São Paulo");   // sem cnpj, sem telefone
        Empresa fundida = cache.upsert(fresca);

        assertThat(fundida).isSameAs(existente);
        assertThat(fundida.getTelefone()).isEqualTo("+5511999990000");   // null não apagou
        verify(repository, never()).findFirstByCnpj(any());
    }

    @Test
    void cnpjNovoCaiNoFallbackNomeCidadeEFundeCadastrais() {
        // Lead cacheado antes pelo Maps, ainda sem CNPJ
        Empresa existente = empresa("Barbearia do Zé", "São Paulo");
        existente.setWebsite("https://barbeariaze.com.br");
        when(repository.findFirstByCnpj("12345678000199")).thenReturn(Optional.empty());
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase("Barbearia do Zé", "São Paulo"))
                .thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa daReceita = empresa("Barbearia do Zé", "São Paulo");
        daReceita.setCnpj("12345678000199");
        daReceita.setRazaoSocial("ZE SANTOS BARBEARIA LTDA");

        Empresa fundida = cache.upsert(daReceita);

        assertThat(fundida).isSameAs(existente);
        assertThat(fundida.getCnpj()).isEqualTo("12345678000199");
        assertThat(fundida.getRazaoSocial()).isEqualTo("ZE SANTOS BARBEARIA LTDA");
        assertThat(fundida.getWebsite()).isEqualTo("https://barbeariaze.com.br");  // preservado
    }

    @Test
    void sinaisFundemSemApagarEProconEhOr() {
        Empresa existente = empresa("Bar X", "Lisboa");
        Sinais sExist = new Sinais();
        sExist.setNotaGoogle(new BigDecimal("4.2"));
        sExist.setProconEviteSite(false);
        existente.definirSinais(sExist);
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase("Bar X", "Lisboa"))
                .thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Bar X", "Lisboa");
        Sinais sNovo = new Sinais();              // sem nota, mas com siteExiste e procon
        sNovo.setSiteExiste(false);
        sNovo.setProconEviteSite(true);
        fresca.definirSinais(sNovo);

        Empresa fundida = cache.upsert(fresca);

        assertThat(fundida.getSinais().getNotaGoogle()).isEqualByComparingTo("4.2");  // não apagada
        assertThat(fundida.getSinais().getSiteExiste()).isFalse();                    // preenchido
        assertThat(fundida.getSinais().isProconEviteSite()).isTrue();                 // OR
    }

    @Test
    void redesFazemUniaoPorRedeEUrl() {
        Empresa existente = empresa("Bar X", "Lisboa");
        existente.adicionarRede(rede("instagram", "https://instagram.com/barx", 100));
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase("Bar X", "Lisboa"))
                .thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Bar X", "Lisboa");
        fresca.adicionarRede(rede("facebook", "https://facebook.com/barx", 300));

        Empresa fundida = cache.upsert(fresca);

        assertThat(fundida.getRedes()).hasSize(2);
        assertThat(fundida.getRedes()).extracting(EmpresaRede::getRede)
                .containsExactlyInAnyOrder("instagram", "facebook");
    }

    private Empresa empresa(String nome, String cidade) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setCidade(cidade);
        return e;
    }

    private EmpresaRede rede(String nome, String url, int seguidores) {
        EmpresaRede r = new EmpresaRede();
        r.setRede(nome);
        r.setUrl(url);
        r.setSeguidores(seguidores);
        return r;
    }
}
