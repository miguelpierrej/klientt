package com.sharcky.klientt.empresa;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * Popula a BD com dados de exemplo no arranque, SE estiver vazia.
 * Temporário (dev): será substituído pelos dados reais do scraper Python.
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
                criar("Barbearia do Zé", "São Paulo", new BigDecimal("3.4"), false, null, false,
                        "instagram", 180),
                criar("Salão Beleza Pura", "São Paulo", new BigDecimal("4.6"), true, 900, false,
                        "instagram", 5200),
                criar("Studio Hair Premium", "São Paulo", new BigDecimal("4.1"), true, 4200, false,
                        "instagram", 820),
                criar("Corte & Cia", "São Paulo", new BigDecimal("2.8"), false, null, true,
                        "facebook", 0),
                criar("Espaço Navalha", "São Paulo", new BigDecimal("4.8"), true, 800, false,
                        "instagram", 12000)
        ));
    }

    private Empresa criar(String nome, String cidade, BigDecimal nota, boolean temSite,
                          Integer velocidadeMs, boolean procon, String rede, int seguidores) {
        Empresa e = new Empresa();
        e.setNome(nome);
        e.setCidade(cidade);
        e.setFonte("seed");

        Sinais s = new Sinais();
        s.setNotaGoogle(nota);
        s.setSiteExiste(temSite);
        s.setSiteVelocidadeMs(velocidadeMs);
        s.setProconEviteSite(procon);
        e.definirSinais(s);

        EmpresaRede r = new EmpresaRede();
        r.setRede(rede);
        r.setSeguidores(seguidores);
        e.adicionarRede(r);

        return e;
    }
}
