package com.sharcky.klientt.enriquecimento.client;

import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoCallback;
import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoRequest;
import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.RedePayload;
import com.sharcky.klientt.scraper.dto.SinaisPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.List;

/**
 * STUB do enriquecimento (dev). Ativo quando klientt.scraper.stub=true (default).
 * Simula o scraper: em background, chama o webhook de enriquecimento com sinais Maps de exemplo
 * para o CNPJ pedido — permite ver o pipeline completo sem o scraper real.
 */
@Component
@ConditionalOnProperty(name = "klientt.scraper.stub", havingValue = "true", matchIfMissing = true)
public class StubEnriquecimentoClient implements EnriquecimentoClient {

    private static final Logger log = LoggerFactory.getLogger(StubEnriquecimentoClient.class);

    private final ScraperProperties properties;
    private final TaskExecutor taskExecutor;
    private final RestClient restClient;

    public StubEnriquecimentoClient(ScraperProperties properties,
                                    @Qualifier("applicationTaskExecutor") TaskExecutor taskExecutor,
                                    RestClient.Builder builder) {
        this.properties = properties;
        this.taskExecutor = taskExecutor;
        this.restClient = builder.build();
    }

    @Override
    public void enriquecer(EnriquecimentoRequest request) {
        taskExecutor.execute(() -> enviarCallback(request));
    }

    private void enviarCallback(EnriquecimentoRequest request) {
        SinaisPayload sinais = new SinaisPayload(
                new BigDecimal("4.3"), 87, true, 1200, true, 5, null, false);
        RedePayload rede = new RedePayload("instagram", "https://instagram.com/exemplo", 850);
        EnriquecimentoCallback callback = new EnriquecimentoCallback(
                request.buscaId(), request.cnpj(), true,
                "[stub] Av. Exemplo, 100 - " + (request.municipio() != null ? request.municipio() : "—"),
                sinais, List.of(rede));
        try {
            restClient.post()
                    .uri(request.callbackUrl())
                    .header(ScraperClient.TOKEN_HEADER, properties.getToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(callback)
                    .retrieve()
                    .toBodilessEntity();
            log.info("[STUB] enriquecimento enviado buscaId={} cnpj={}", request.buscaId(), request.cnpj());
        } catch (Exception ex) {
            log.error("[STUB] falha ao enviar enriquecimento cnpj={}", request.cnpj(), ex);
        }
    }
}
