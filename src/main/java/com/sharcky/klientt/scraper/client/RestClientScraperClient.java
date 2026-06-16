package com.sharcky.klientt.scraper.client;

import com.sharcky.klientt.scraper.config.ScraperProperties;
import com.sharcky.klientt.scraper.dto.ScrapeAck;
import com.sharcky.klientt.scraper.dto.ScrapeRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;

/**
 * Implementação real: chama o scraper via HTTP (CONTRATO-SCRAPER.md §2).
 * Ativa quando klientt.scraper.stub=false.
 */
@Component
@ConditionalOnProperty(name = "klientt.scraper.stub", havingValue = "false")
public class RestClientScraperClient implements ScraperClient {

    private final RestClient restClient;
    private final ScraperProperties properties;

    public RestClientScraperClient(RestClient.Builder builder, ScraperProperties properties) {
        this.properties = properties;
        HttpClient httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.restClient = builder
                .requestFactory(new JdkClientHttpRequestFactory(httpClient))
                .baseUrl(properties.getBaseUrl())
                .build();
    }

    @Override
    public ScrapeAck iniciarBusca(ScrapeRequest request) {
        return restClient.post()
                .uri("/v1/scrapes")
                .header(TOKEN_HEADER, properties.getToken())
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(ScrapeAck.class);
    }
}
