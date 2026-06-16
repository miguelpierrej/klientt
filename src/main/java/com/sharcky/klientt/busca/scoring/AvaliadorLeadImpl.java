package com.sharcky.klientt.busca.scoring;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.procon.service.ProconService;
import org.springframework.stereotype.Component;

/**
 * Implementação da heurística de score (ARQUITETURA §6).
 * Concentra num só lugar a derivação dos sinais e a pontuação, para que o
 * serviço de busca e o mapper não dupliquem regras de negócio.
 */
@Component
public class AvaliadorLeadImpl implements AvaliadorLead {

    private static final int SITE_LENTO_MS = 3000;
    private static final double NOTA_BAIXA = 4.0;
    private static final int POUCOS_SEGUIDORES = 500;

    private final ProconService proconService;

    public AvaliadorLeadImpl(ProconService proconService) {
        this.proconService = proconService;
    }

    @Override
    public AvaliacaoLead avaliar(Empresa empresa) {
        Sinais s = empresa.getSinais();

        double nota = (s != null && s.getNotaGoogle() != null) ? s.getNotaGoogle().doubleValue() : 0.0;
        boolean temSite = s != null && Boolean.TRUE.equals(s.getSiteExiste());
        boolean siteLento = s != null && s.getSiteVelocidadeMs() != null && s.getSiteVelocidadeMs() > SITE_LENTO_MS;
        // Flag enviada pelo scraper OU domínio na lista Procon sincronizada localmente.
        boolean procon = (s != null && s.isProconEviteSite())
                || proconService.constaNoProcon(empresa.getWebsite());

        // Só pontuamos "poucos seguidores" quando há mesmo dados de redes. Enquanto o
        // scraper não deteta perfis, uma lista de redes vazia significa "não recolhido"
        // — não "sem presença" — e não deve somar +15 (senão todos os leads pontuariam igual).
        boolean seguidoresConhecidos = empresa.getRedes().stream()
                .anyMatch(r -> r.getSeguidores() != null);
        int seguidores = empresa.getRedes().stream()
                .mapToInt(r -> r.getSeguidores() != null ? r.getSeguidores() : 0)
                .sum();

        int score = calcularScore(temSite, siteLento, nota, seguidoresConhecidos, seguidores, procon);
        return new AvaliacaoLead(nota, temSite, siteLento, seguidores, seguidoresConhecidos, procon, score);
    }

    private int calcularScore(boolean temSite, boolean siteLento, double nota,
                              boolean seguidoresConhecidos, int seguidores, boolean procon) {
        int score = 0;
        if (!temSite) score += 30;
        if (siteLento) score += 20;
        if (nota < NOTA_BAIXA) score += 15;
        if (seguidoresConhecidos && seguidores < POUCOS_SEGUIDORES) score += 15;
        if (procon) score += 25;
        return score;
    }
}
