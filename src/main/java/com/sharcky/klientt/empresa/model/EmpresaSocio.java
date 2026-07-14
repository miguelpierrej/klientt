package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Sócio de uma empresa (QSA da Receita), recolhido na descoberta (Minha Receita).
 */
@Entity
@Table(name = "empresa_socios")
@Getter
@Setter
public class EmpresaSocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 120)
    private String qualificacao;

    @Column(name = "faixa_etaria", length = 60)
    private String faixaEtaria;

    private LocalDate desde;
}
