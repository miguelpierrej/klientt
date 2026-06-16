package com.sharcky.klientt.busca.job;

import com.sharcky.klientt.busca.dto.TipoBusca;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job de busca: representa um pedido do utilizador, processado de forma assíncrona
 * (o id do job é o buscaId enviado ao scraper — CONTRATO-SCRAPER.md).
 */
@Entity
@Table(name = "jobs_busca")
@Getter
@Setter
public class JobBusca {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Dono do job — nullable até existir autenticação (Fase 4). */
    @Column(name = "utilizador_id")
    private Long utilizadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoBusca tipo;

    @Column(nullable = false, length = 255)
    private String termo;

    @Column(length = 120)
    private String regiao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoJob estado = EstadoJob.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;

    /** Nº de fontes assíncronas que têm de reportar antes de concluir (dual-fonte: scraper + CNPJ). */
    @Column(name = "fontes_esperadas", nullable = false)
    private int fontesEsperadas = 1;

    /** Nº de fontes que já reportaram. */
    @Column(name = "fontes_concluidas", nullable = false)
    private int fontesConcluidas = 0;
}
