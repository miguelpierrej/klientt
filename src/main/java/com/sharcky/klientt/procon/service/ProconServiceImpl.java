package com.sharcky.klientt.procon.service;

import com.sharcky.klientt.procon.client.ProconEviteSitesFonte;
import com.sharcky.klientt.procon.client.ProconRegisto;
import com.sharcky.klientt.procon.config.ProconProperties;
import com.sharcky.klientt.procon.model.ProconEviteSite;
import com.sharcky.klientt.procon.repository.ProconEviteSiteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Mantém em memória o conjunto de domínios Procon "Evite Sites" e responde
 * lookups O(1) durante o scoring. Sincroniza a partir da fonte (quando ligada)
 * num job agendado e recarrega o cache; no arranque carrega o que já está em BD.
 */
@Service
public class ProconServiceImpl implements ProconService {

    private static final Logger log = LoggerFactory.getLogger(ProconServiceImpl.class);

    private final ProconEviteSiteRepository repository;
    private final ProconProperties properties;
    private final Optional<ProconEviteSitesFonte> fonte;

    /** Cópia imutável trocada por inteiro a cada recarga — leitura sem locks. */
    private volatile Set<String> dominios = Set.of();

    public ProconServiceImpl(ProconEviteSiteRepository repository, ProconProperties properties,
                             Optional<ProconEviteSitesFonte> fonte) {
        this.repository = repository;
        this.properties = properties;
        this.fonte = fonte;
    }

    @Override
    public boolean constaNoProcon(String website) {
        String dominio = dominioDe(website);
        return dominio != null && dominios.contains(dominio);
    }

    /** No arranque, popula o cache com o que já existe em BD (sem ir à rede). */
    @EventListener(ApplicationReadyEvent.class)
    public void carregarAoArrancar() {
        recarregarCache();
    }

    @Scheduled(cron = "${klientt.procon.cron:0 0 4 * * *}")
    @Transactional
    public void sincronizar() {
        if (!properties.isEnabled() || fonte.isEmpty()) {
            return;
        }
        try {
            List<ProconRegisto> registos = fonte.get().obter();
            registos.forEach(this::guardar);
            recarregarCache();
            log.info("Lista Procon sincronizada: {} domínios em cache", dominios.size());
        } catch (Exception ex) {
            log.error("Falha ao sincronizar a lista Procon", ex);
        }
    }

    private void guardar(ProconRegisto registo) {
        String dominio = dominioDe(registo.dominio());
        if (dominio == null) {
            return;
        }
        ProconEviteSite entidade = repository.findByDominio(dominio).orElseGet(ProconEviteSite::new);
        entidade.setDominio(dominio);
        entidade.setRazaoSocial(registo.razaoSocial());
        entidade.setCnpj(registo.cnpj());
        entidade.setSincronizadoEm(LocalDateTime.now());
        repository.save(entidade);
    }

    private void recarregarCache() {
        this.dominios = repository.findAll().stream()
                .map(ProconEviteSite::getDominio)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet());
    }

    /** Extrai o domínio (sem esquema e sem "www.") de um URL ou domínio cru. */
    static String dominioDe(String urlOuDominio) {
        if (urlOuDominio == null || urlOuDominio.isBlank()) {
            return null;
        }
        String valor = urlOuDominio.trim().toLowerCase();
        if (!valor.contains("://")) {
            valor = "http://" + valor;
        }
        try {
            String host = URI.create(valor).getHost();
            if (host == null) {
                return null;
            }
            return host.startsWith("www.") ? host.substring(4) : host;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
