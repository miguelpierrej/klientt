package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        existente.setEmail("contato@ze.com.br");      // já tinha email
        when(repository.findFirstByCnpj("12345678000199")).thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Recoleta com o mesmo CNPJ, sem email mas com website
        Empresa fresca = empresa("Barbearia do Zé", "São Paulo");
        fresca.setCnpj("12345678000199");
        fresca.setWebsite("https://barbeariaze.com.br");

        Empresa fundida = cache.upsert(fresca);

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
        // Lead cacheado antes, ainda sem CNPJ
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
    void empresaNovaGeraContatosDeTelefoneEEmail() {
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(any(), any())).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Bar X", "Lisboa");
        fresca.setTelefone("+351910000000");
        fresca.setEmail("contato@barx.test");
        fresca.setFonte("casadosdados");

        Empresa salva = cache.upsert(fresca);

        assertThat(salva.getContatos()).extracting(Contato::getTipo)
                .containsExactlyInAnyOrder("telefone", "email");
        assertThat(salva.isContactavel()).isTrue();
    }

    @Test
    void contatosFazemUniaoSemDuplicar() {
        Empresa existente = empresa("Bar X", "Lisboa");      // já tinha telefone
        Contato tel = new Contato();
        tel.setTipo("telefone");
        tel.setValor("+351910000000");
        existente.adicionarContato(tel);
        when(repository.findFirstByNomeIgnoreCaseAndCidadeIgnoreCase("Bar X", "Lisboa"))
                .thenReturn(Optional.of(existente));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Empresa fresca = empresa("Bar X", "Lisboa");      // mesmo telefone + email novo
        fresca.setTelefone("+351910000000");
        fresca.setEmail("contato@barx.test");

        Empresa fundida = cache.upsert(fresca);

        assertThat(fundida.getContatos()).extracting(Contato::getTipo)
                .containsExactlyInAnyOrder("telefone", "email");   // telefone não duplicou
    }

    private Empresa empresa(String nome, String cidade) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setCidade(cidade);
        return e;
    }
}
