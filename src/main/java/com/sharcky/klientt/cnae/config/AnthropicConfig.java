package com.sharcky.klientt.cnae.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cria o cliente Anthropic apenas quando o fallback nicho→CNAE está ligado
 * (klientt.cnae.enabled=true). Sem isto, não há bean e o resolvedor usa só a tabela.
 */
@Configuration
public class AnthropicConfig {

    @Bean
    @ConditionalOnProperty(name = "klientt.cnae.enabled", havingValue = "true")
    public AnthropicClient anthropicClient(CnaeProperties properties) {
        return AnthropicOkHttpClient.builder()
                .apiKey(properties.getApiKey())
                .build();
    }
}
