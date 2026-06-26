package com.sharcky.klientt.empresa;

import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Popula a BD com dados de exemplo no arranque, SE estiver vazia.
 * Temporário (dev): será substituído pelos dados reais da Casa dos Dados.
 * Em H2 em memória corre sempre (BD nova a cada arranque); em MySQL só na 1ª vez.
 */
@Component
public class DevDataSeeder implements ApplicationRunner {

    private final EmpresaRepository empresaRepository;

    public DevDataSeeder(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (empresaRepository.count() > 0) {
            return;
        }

        empresaRepository.saveAll(List.of(
                criar("Barbearia do Zé", "São Paulo", "12345678000199", "MICRO EMPRESA",
                        LocalDate.of(2021, 3, 10), "+5511990000001", "contato@barbeariadoze.com.br"),
                criar("Salão Beleza Pura", "São Paulo", "22345678000188", "MICRO EMPRESA",
                        LocalDate.of(2018, 7, 1), "+5511990000002", null),
                criar("Studio Hair Premium", "São Paulo", "32345678000177", "EMPRESA DE PEQUENO PORTE",
                        LocalDate.of(2023, 1, 20), null, "studio@hairpremium.com.br"),
                criar("Corte & Cia", "São Paulo", "42345678000166", "MICRO EMPRESA",
                        LocalDate.of(2015, 11, 5), null, null),
                criar("Espaço Navalha", "São Paulo", "52345678000155", "MICRO EMPRESA",
                        LocalDate.of(2024, 2, 14), "+5511990000005", "ola@espaconavalha.com.br")
        ));
    }

    private Empresa criar(String nome, String cidade, String cnpj, String porte,
                          LocalDate dataAbertura, String telefone, String email) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setCidade(cidade);
        e.setCnpj(cnpj);
        e.setPorte(porte);
        e.setSituacaoCadastral("ATIVA");
        e.setDataAbertura(dataAbertura);
        e.setTelefone(telefone);
        e.setEmail(email);
        if (telefone != null) {
            e.adicionarContato(contato("telefone", telefone));
        }
        if (email != null) {
            e.adicionarContato(contato("email", email));
        }
        return e;
    }

    private Contato contato(String tipo, String valor) {
        Contato c = new Contato();
        c.setTipo(tipo);
        c.setValor(valor);
        return c;
    }
}
