package com.sharcky.klientt.enriquecimento;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.enriquecimento.config.ScraperProperties;
import com.sharcky.klientt.enriquecimento.dto.EnrichRequestDto;
import com.sharcky.klientt.enriquecimento.dto.EnrichRequestDto.EmpresaEnrichDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

/**
 * Cliente do serviço de enriquecimento (scraper). Envia a lista descoberta e devolve se o
 * pedido foi <b>despachado</b> (202). O resultado chega mais tarde por callback
 * ({@link com.sharcky.klientt.enriquecimento.web.ScraperCallbackController}).
 *
 * <p>Nunca lança: qualquer falha devolve {@code false} para o chamador poder concluir o job
 * na mesma (falha graciosa — o enriquecimento é um extra, não bloqueia a lista).
 */
@Service
public class ScraperClient {

    private static final Logger log = LoggerFactory.getLogger(ScraperClient.class);

    private final ScraperProperties properties;
    private final RestClient restClient;

    public ScraperClient(ScraperProperties properties) {
        this.properties = properties;
        // SimpleClientHttpRequestFactory (HttpURLConnection): o factory por default do Boot 4
        // (JDK HttpClient) não entrega o corpo ao uvicorn/FastAPI → 422 "body missing".
        this.restClient = properties.isConfigurado()
                ? RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(fabrica()).build()
                : null;
    }

    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(10_000);
        f.setReadTimeout(15_000);
        return f;
    }

    /**
     * Envia as empresas descobertas para enriquecimento.
     *
     * @return {@code true} se o scraper aceitou o pedido (o callback concluirá o job);
     *         {@code false} se está desligado, a lista é vazia, ou o envio falhou.
     */
    public boolean enriquecer(Long jobId, List<EmpresaPayload> empresas) {
        if (!properties.isConfigurado() || empresas == null || empresas.isEmpty()) {
            return false;
        }
        List<EmpresaEnrichDto> itens = empresas.stream().map(ScraperClient::toItem).toList();
        EnrichRequestDto pedido = new EnrichRequestDto(
                String.valueOf(jobId), itens, properties.callbackUrl(),
                properties.isColetarEmails(), properties.isUsarMaps(), properties.isVerificarSmtp(),
                properties.getTamanhoLote());
        try {
            restClient.post()
                    .uri("/v1/enrich")
                    .header("X-Klientt-Token", properties.getToken())
                    .body(pedido)
                    .retrieve()
                    .toBodilessEntity();
            log.info("Enriquecimento despachado: job={} empresas={}", jobId, itens.size());
            return true;
        } catch (Exception ex) {
            log.warn("Falha ao despachar enriquecimento job={}: {}", jobId, ex.getMessage());
            return false;
        }
    }

    private static EmpresaEnrichDto toItem(EmpresaPayload e) {
        String fantasia = e.cadastrais() != null ? e.cadastrais().nomeFantasia() : null;
        return new EmpresaEnrichDto(e.cnpj(), e.nome(), fantasia, e.cidade(), null, e.website());
    }
}
