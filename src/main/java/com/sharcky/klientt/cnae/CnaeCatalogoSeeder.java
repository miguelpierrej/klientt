package com.sharcky.klientt.cnae;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Carrega o catálogo CNAE (subclasses do IBGE) do recurso cnae/subclasses.csv para a tabela,
 * se ainda estiver vazia. Em H2 (memória) corre a cada arranque; em MySQL só na 1ª vez.
 */
@Component
public class CnaeCatalogoSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CnaeCatalogoSeeder.class);
    private static final String RECURSO = "cnae/subclasses.csv";

    private final CnaeCatalogoRepository repository;

    public CnaeCatalogoSeeder(CnaeCatalogoRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (repository.count() > 0) {
            return;
        }
        List<CnaeCatalogo> entradas = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(new ClassPathResource(RECURSO).getInputStream(), StandardCharsets.UTF_8))) {
            String linha;
            while ((linha = br.readLine()) != null) {
                int tab = linha.indexOf('\t');
                if (tab <= 0) {
                    continue;
                }
                CnaeCatalogo c = new CnaeCatalogo();
                c.setCodigo(linha.substring(0, tab).trim());
                c.setDescricao(linha.substring(tab + 1).trim());
                entradas.add(c);
            }
        } catch (Exception ex) {
            log.error("Falha ao carregar o catálogo CNAE de {}", RECURSO, ex);
            return;
        }
        repository.saveAll(entradas);
        log.info("Catálogo CNAE carregado: {} subclasses", entradas.size());
    }
}
