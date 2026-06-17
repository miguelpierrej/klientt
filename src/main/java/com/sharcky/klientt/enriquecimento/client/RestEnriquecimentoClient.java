package com.sharcky.klientt.enriquecimento.client;

import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoRequest;
import com.sharcky.klientt.scraper.client.ScraperClient;
import com.sharcky.klientt.scraper.config.ScraperProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Cliente real do enriquecimento: POST {scraper}/v1/enrich. Ativo quando klientt.scraper.stub=false.
 */
@Component
@ConditionalOnProperty(name = "klientt.scraper.stub", havingValue = "false")
public class RestEnriquecimentoClient implements EnriquecimentoClient {

    private final ScraperProperties properties;
    private final RestClient restClient;

    public RestEnriquecimentoClient(ScraperProperties properties, RestClient.Builder builder) {
        this.properties = properties;
        this.restClient = builder.baseUrl(properties.getBaseUrl()).build();
    }

    @Override
    public void enriquecer(EnriquecimentoRequest request) {
        restClient.post()
                .uri(properties.getEnriquecimentoPath())
                .header(ScraperClient.TOKEN_HEADER, properties.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }
}
