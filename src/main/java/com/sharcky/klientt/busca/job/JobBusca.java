package com.sharcky.klientt.busca.job;

import com.sharcky.klientt.busca.dto.TipoBusca;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Job de busca: representa um pedido do utilizador, processado de forma assíncrona
 * (descoberta na Casa dos Dados — PLANO-SO-API.md).
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

    /** CNAE confirmado (NICHO) — para "carregar mais" continuar a mesma busca. */
    @Column(length = 20)
    private String cnae;

    /** Cursor da fonte (Minha Receita) para a próxima página; null = não há mais.
     *  Coluna cursor_fonte porque {@code cursor} é palavra reservada no MySQL. */
    @Column(name = "cursor_fonte", length = 100)
    private String cursor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoJob estado = EstadoJob.PENDENTE;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "concluido_em")
    private LocalDateTime concluidoEm;
}
