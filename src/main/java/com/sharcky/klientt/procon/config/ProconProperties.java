package com.sharcky.klientt.procon.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuração da sincronização da lista Procon-SP "Evite Sites" (ARQUITETURA §2.4).
 * Por default fica desligada (enabled=false): a app arranca e pontua à mesma — só a
 * verificação por domínio fica inativa até se ligar uma fonte real.
 */
@Component
@ConfigurationProperties(prefix = "klientt.procon")
@Getter
@Setter
public class ProconProperties {

    /** Se true, o job agendado vai buscar e sincronizar a lista oficial. */
    private boolean enabled = false;

    /** URL da lista oficial Procon-SP "Evite Sites". */
    private String url = "https://sistemas.procon.sp.gov.br/evitesite/list/evitesites.php";

    /** Expressão cron da sincronização (default: diária às 04:00). */
    private String cron = "0 0 4 * * *";
}
