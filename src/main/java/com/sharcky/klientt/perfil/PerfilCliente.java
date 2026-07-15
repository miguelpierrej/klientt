package com.sharcky.klientt.perfil;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Perfil do cliente (ICP) — o que ele vende e quem quer alcançar. 1:1 com o utilizador
 * ({@code utilizador_id} é a PK). Alimenta o onboarding e o score de relevância dos leads.
 */
@Entity
@Table(name = "perfil_cliente")
@Getter
@Setter
public class PerfilCliente {

    @Id
    @Column(name = "utilizador_id")
    private Long utilizadorId;

    /** O que o cliente vende/oferece (contexto livre). */
    private String oferta;

    /** CNAEs-alvo, separados por vírgula. */
    @Column(name = "nichos_alvo", length = 500)
    private String nichosAlvo;

    /** Regiões-alvo ("Cidade/UF" ou UF), separadas por vírgula. */
    @Column(name = "regioes_alvo", length = 500)
    private String regioesAlvo;

    /** Portes-alvo (MEI,MICRO,PEQUENA,GRANDE), separados por vírgula. */
    @Column(name = "portes_alvo", length = 120)
    private String portesAlvo;

    @Column(name = "quer_sem_site", nullable = false)
    private boolean querSemSite = false;

    @Column(name = "quer_simples_mei", nullable = false)
    private boolean querSimplesMei = false;

    @Column(name = "quer_com_contato", nullable = false)
    private boolean querComContato = true;

    /** Onboarding preenchido ou pulado (para não voltar a redirecionar). */
    @Column(nullable = false)
    private boolean concluido = false;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public List<String> nichos() {
        return lista(nichosAlvo);
    }

    public List<String> regioes() {
        return lista(regioesAlvo);
    }

    public List<String> portes() {
        return lista(portesAlvo);
    }

    /** Tem algum critério de ICP definido (para saber se vale aplicar o score de fit)? */
    public boolean temAlvo() {
        return !nichos().isEmpty() || !regioes().isEmpty() || !portes().isEmpty()
                || querSemSite || querSimplesMei;
    }

    private static List<String> lista(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
