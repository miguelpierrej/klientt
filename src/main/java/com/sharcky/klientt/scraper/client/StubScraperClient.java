package com.sharcky.klientt.scraper.client;

import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * STUB do scraper (dev). Ativo quando klientt.scraper.stub=true (default).
 *
 * Simula o serviço real: responde ACEITE de imediato e, em background, chama o
 * webhook do Klientt (callbackUrl) com empresas de exemplo — exercitando o ciclo
 * completo da integração sem depender do scraper real do colega de equipa.
 */
@Component
@ConditionalOnProperty(name = "klientt.scraper.stub", havingValue = "true", matchIfMissing = true)
public class StubScraperClient implements ScraperClient {

    private static final Logger log = LoggerFactory.getLogger(StubScraperClient.class);

    private final ScraperProperties properties;
    private final TaskExecutor taskExecutor;
    private final RestClient restClient;

    public StubScraperClient(ScraperProperties properties,
                             TaskExecutor taskExecutor,
                             RestClient.Builder builder) {
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.restClient = builder.build();
    }

    @Override
    public ScrapeAck iniciarBusca(ScrapeRequest request) {
        log.info("[STUB] busca recebida buscaId={} termo='{}' regiao='{}'",
                request.buscaId(), request.termo(), request.regiao());

        taskExecutor.execute(() -> enviarCallback(request));

        return new ScrapeAck("stub-" + UUID.randomUUID(), request.buscaId(), EstadoScrape.ACEITE);
    }

    private void enviarCallback(ScrapeRequest request) {
        ScrapeCallback callback = new ScrapeCallback(
                request.buscaId(), EstadoScrape.CONCLUIDO, null, exemplos(request));
        try {
            restClient.post()
                    .uri(request.callbackUrl())
                    .header(TOKEN_HEADER, properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(callback)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[STUB] callback enviado buscaId={} ({} empresas)",
                    request.buscaId(), callback.empresas().size());
        } catch (Exception ex) {
            log.error("[STUB] falha ao enviar callback buscaId={}", request.buscaId(), ex);
        }
    }

    private List<EmpresaPayload> exemplos(ScrapeRequest request) {
        String cidade = (request.regiao() == null || request.regiao().isBlank())
                ? "São Paulo" : request.regiao();
        String termo = request.termo();
        return List.of(
                empresa(termo + " - Unidade Centro", cidade, new BigDecimal("3.2"), false, null, true,
                        "instagram", 120),
                empresa(termo + " - Filial Jardins", cidade, new BigDecimal("4.7"), true, 850, false,
                        "instagram", 6400)
        );
    }

    private EmpresaPayload empresa(String nome, String cidade, BigDecimal nota, boolean temSite,
                                   Integer velocidadeMs, boolean procon, String rede, int seguidores) {
        SinaisPayload sinais = new SinaisPayload(
                nota, null, temSite, velocidadeMs, temSite ? Boolean.TRUE : null, null, null, procon);
        RedePayload redePayload = new RedePayload(rede, "https://" + rede + ".com/exemplo", seguidores);
        return new EmpresaPayload(nome, null, null, null, cidade, null, null, null,
                "stub", sinais, List.of(redePayload));
    }
}
