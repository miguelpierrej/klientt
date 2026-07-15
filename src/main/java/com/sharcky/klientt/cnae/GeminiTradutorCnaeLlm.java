package com.sharcky.klientt.cnae;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.sharcky.klientt.cnae.config.CnaeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Fallback nicho→CNAE via <b>Gemini</b> (Google Generative Language API).
 * Ativo só com {@code klientt.cnae.enabled=true}. Pede JSON estrito
 * ({@code responseMimeType=application/json}) e tolera falhas (devolve vazio em erro —
 * a busca continua, apenas sem este nicho resolvido pelo LLM).
 */
@Component
@ConditionalOnProperty(name = "klientt.cnae.enabled", havingValue = "true")
public class GeminiTradutorCnaeLlm implements TradutorCnaeLlm {

    private static final Logger log = LoggerFactory.getLogger(GeminiTradutorCnaeLlm.class);

    private static final String INSTRUCAO = """
            És um classificador de atividades económicas do Brasil. Dado um termo de busca \
            (nicho ou tipo de negócio), devolve os códigos CNAE mais prováveis.
            Responde APENAS com JSON, sem texto à volta, no formato:
            {"sugestoes":[{"cnae":"0000-0/00","descricao":"...","confianca":0.0}]}
            Devolve no máximo 3 sugestões, ordenadas por confiança (0.0–1.0). Se não souberes, \
            devolve {"sugestoes":[]}.

            Termo:\s""";

    // Parser próprio (Jackson 2): o app corre em Jackson 3 (tools.jackson) e não expõe um
    // bean com.fasterxml ObjectMapper — instanciamos localmente para o JSON pequeno da resposta.
    private static final ObjectMapper MAPPER = new JsonMapper();

    private final CnaeProperties properties;
    private final RestClient restClient;

    public GeminiTradutorCnaeLlm(CnaeProperties properties) {
        this.properties = properties;
        this.restClient = RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(fabrica()).build();
    }

    /** Timeouts: se o Gemini demorar/pendurar, falha rápido e cai para o catálogo (não trava o modal). */
    private static SimpleClientHttpRequestFactory fabrica() {
        SimpleClientHttpRequestFactory f = new SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5_000);
        f.setReadTimeout(12_000);
        return f;
    }

    @Override
    public List<Cnae> traduzir(String termo) {
        try {
            Map<String, Object> corpo = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", INSTRUCAO + termo)))),
                    // thinkingBudget=0: desliga o "pensamento" do Gemini 2.5-flash (senão consome o
                    // orçamento de saída e trunca o JSON) + maxOutputTokens folgado para o JSON caber.
                    "generationConfig", Map.of(
                            "responseMimeType", "application/json",
                            "temperature", 0,
                            "maxOutputTokens", 1024,
                            "thinkingConfig", Map.of("thinkingBudget", 0)));

            // Lê como String e faz parse localmente (evita depender do conversor Jackson do Spring).
            String corpoResposta = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent", properties.getModel())
                    .header("x-goog-api-key", properties.getApiKey())
                    .body(corpo)
                    .retrieve()
                    .body(String.class);

            if (corpoResposta == null || corpoResposta.isBlank()) {
                return List.of();
            }
            String texto = MAPPER.readTree(corpoResposta)
                    .path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            return parse(texto);
        } catch (Exception ex) {
            log.warn("Falha ao traduzir termo->CNAE via Gemini (termo='{}'): {}", termo, ex.getMessage());
            return List.of();
        }
    }

    private List<Cnae> parse(String texto) throws Exception {
        String json = extrairJson(texto);
        if (json.isBlank()) {
            return List.of();
        }
        JsonNode raiz = MAPPER.readTree(json);
        List<Cnae> resultado = new ArrayList<>();
        for (JsonNode no : raiz.path("sugestoes")) {
            String codigo = no.path("cnae").asText("").trim();
            String descricao = no.path("descricao").asText("").trim();
            if (!codigo.isEmpty()) {
                resultado.add(new Cnae(codigo, descricao));
            }
        }
        return resultado;
    }

    /** Remove cercas markdown (```json … ```) caso o modelo as inclua apesar do JSON mode. */
    private String extrairJson(String texto) {
        String t = texto.trim();
        if (t.startsWith("```")) {
            int inicio = t.indexOf('\n');
            int fim = t.lastIndexOf("```");
            if (inicio >= 0 && fim > inicio) {
                return t.substring(inicio + 1, fim).trim();
            }
        }
        return t;
    }
}
