package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Sinais enriquecidos de uma empresa (ARQUITETURA §5).
 * Lado dono da relação 1-1 (contém a FK empresa_id).
 */
@Entity
@Table(name = "sinais")
@Getter
@Setter
public class Sinais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(name = "nota_google", precision = 2, scale = 1)
    private BigDecimal notaGoogle;

    @Column(name = "num_reviews")
    private Integer numReviews;

    @Column(name = "site_existe")
    private Boolean siteExiste;

    @Column(name = "site_velocidade_ms")
    private Integer siteVelocidadeMs;

    @Column(name = "site_https")
    private Boolean siteHttps;

    @Column(name = "site_num_paginas")
    private Integer siteNumPaginas;

    @Column(name = "site_reputacao", length = 50)
    private String siteReputacao;

    @Column(name = "procon_evite_site", nullable = false)
    private boolean proconEviteSite;

    @Column(name = "coletado_em", nullable = false)
    private LocalDateTime coletadoEm = LocalDateTime.now();
}
