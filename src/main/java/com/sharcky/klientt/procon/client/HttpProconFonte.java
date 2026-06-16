package com.sharcky.klientt.procon.client;

import com.sharcky.klientt.procon.config.ProconProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fonte real: descarrega a página da lista Procon-SP e extrai os domínios.
 * Ativa só com klientt.procon.enabled=true.
 *
 * <p>NOTA: a extração por regex é deliberadamente conservadora e <b>precisa de ser
 * calibrada contra a estrutura HTML real da página</b> (que pode mudar). Domínios
 * oficiais/genéricos são excluídos para reduzir falsos positivos.
 */
@Component
@ConditionalOnProperty(name = "klientt.procon.enabled", havingValue = "true")
public class HttpProconFonte implements ProconEviteSitesFonte {

    private static final Pattern DOMINIO = Pattern.compile(
            "(?:https?://)?(?:www\\.)?([a-z0-9](?:[a-z0-9-]*[a-z0-9])?(?:\\.[a-z0-9-]+)+)",
            Pattern.CASE_INSENSITIVE);

    /** Domínios da própria infraestrutura/genéricos que não são "sites a evitar". */
    private static final Set<String> IGNORAR = Set.of(
            "procon.sp.gov.br", "sp.gov.br", "gov.br", "google.com", "w3.org", "jquery.com");

    private final RestClient restClient;
    private final ProconProperties properties;

    public HttpProconFonte(RestClient.Builder builder, ProconProperties properties) {
        this.properties = properties;
        this.restClient = builder.build();
    }

    @Override
    public List<ProconRegisto> obter() {
        String corpo = restClient.get()
                .uri(properties.getUrl())
                .retrieve()
                .body(String.class);
        return extrairDominios(corpo);
    }

    private List<ProconRegisto> extrairDominios(String corpo) {
        if (corpo == null || corpo.isBlank()) {
            return List.of();
        }
        Set<String> dominios = new LinkedHashSet<>();
        Matcher m = DOMINIO.matcher(corpo);
        while (m.find()) {
            String dominio = m.group(1).toLowerCase();
            if (!IGNORAR.contains(dominio)) {
                dominios.add(dominio);
            }
        }
        List<ProconRegisto> registos = new ArrayList<>(dominios.size());
        for (String dominio : dominios) {
            registos.add(new ProconRegisto(dominio, null, null));
        }
        return registos;
    }
}
