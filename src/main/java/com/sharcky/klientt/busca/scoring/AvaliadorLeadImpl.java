package com.sharcky.klientt.busca.scoring;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.Sinais;
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

    @Override
    public AvaliacaoLead avaliar(Empresa empresa) {
        Sinais s = empresa.getSinais();

        double nota = (s != null && s.getNotaGoogle() != null) ? s.getNotaGoogle().doubleValue() : 0.0;
        boolean temSite = s != null && Boolean.TRUE.equals(s.getSiteExiste());
        boolean siteLento = s != null && s.getSiteVelocidadeMs() != null && s.getSiteVelocidadeMs() > SITE_LENTO_MS;
        boolean procon = s != null && s.isProconEviteSite();
        int seguidores = empresa.getRedes().stream()
                .mapToInt(r -> r.getSeguidores() != null ? r.getSeguidores() : 0)
                .sum();

        int score = calcularScore(temSite, siteLento, nota, seguidores, procon);
        return new AvaliacaoLead(nota, temSite, siteLento, seguidores, procon, score);
    }

    private int calcularScore(boolean temSite, boolean siteLento, double nota, int seguidores, boolean procon) {
        int score = 0;
        if (!temSite) score += 30;
        if (siteLento) score += 20;
        if (nota < NOTA_BAIXA) score += 15;
        if (seguidores < POUCOS_SEGUIDORES) score += 15;
        if (procon) score += 25;
        return score;
    }
}
