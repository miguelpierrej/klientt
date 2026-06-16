package com.sharcky.klientt.cnae;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sharcky.klientt.cnae.config.CnaeProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Fallback nicho→CNAE via Claude (PLANO-DUAL-FONTE.md, Fase D).
 * Ativo só com klientt.cnae.enabled=true. Pede JSON estrito e tolera falhas
 * (devolve vazio em erro — a busca continua, apenas sem este nicho resolvido).
 */
@Component
@ConditionalOnProperty(name = "klientt.cnae.enabled", havingValue = "true")
public class ClaudeTradutorCnaeLlm implements TradutorCnaeLlm {

    private static final Logger log = LoggerFactory.getLogger(ClaudeTradutorCnaeLlm.class);

    private static final String INSTRUCAO = """
            És um classificador de atividades económicas do Brasil. Dado um termo de busca \
            (nicho ou tipo de negócio), devolve os códigos CNAE mais prováveis.
            Responde APENAS com JSON, sem texto à volta, no formato:
            {"sugestoes":[{"cnae":"0000-0/00","descricao":"...","confianca":0.0}]}
            Devolve no máximo 3 sugestões, ordenadas por confiança (0.0–1.0). Se não souberes, \
            devolve {"sugestoes":[]}.

            Termo:\s""";

    private final AnthropicClient client;
    private final CnaeProperties properties;
    private final ObjectMapper objectMapper;

    public ClaudeTradutorCnaeLlm(AnthropicClient client, CnaeProperties properties, ObjectMapper objectMapper) {
        this.client = client;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Cnae> traduzir(String termo) {
        try {
            MessageCreateParams params = MessageCreateParams.builder()
                    .model(properties.getModel())
                    .maxTokens(1024L)
                    .addUserMessage(INSTRUCAO + termo)
                    .build();

            String texto = client.messages().create(params).content().stream()
                    .flatMap(bloco -> bloco.text().stream())
                    .map(t -> t.text())
                    .collect(Collectors.joining());

            return parse(texto);
        } catch (Exception ex) {
            log.warn("Falha ao traduzir termo->CNAE via Claude (termo='{}'): {}", termo, ex.getMessage());
            return List.of();
        }
    }

    private List<Cnae> parse(String texto) throws Exception {
        String json = extrairJson(texto);
        JsonNode raiz = objectMapper.readTree(json);
        JsonNode sugestoes = raiz.path("sugestoes");
        List<Cnae> resultado = new ArrayList<>();
        for (JsonNode no : sugestoes) {
            String codigo = no.path("cnae").asText("").trim();
            String descricao = no.path("descricao").asText("").trim();
            if (!codigo.isEmpty()) {
                resultado.add(new Cnae(codigo, descricao));
            }
        }
        return resultado;
    }

    /** Remove cercas markdown (```json … ```) se o modelo as incluir. */
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
