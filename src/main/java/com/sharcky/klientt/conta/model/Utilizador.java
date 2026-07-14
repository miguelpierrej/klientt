package com.sharcky.klientt.conta.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Utilizador da aplicação. Autenticação por email + password (BCrypt).
 */
@Entity
@Table(name = "utilizadores")
@Getter
@Setter
public class Utilizador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nome;

    @Column(nullable = false, length = 255, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plano_id")
    private Plano plano;

    /** Saldo de leads comprados (créditos pré-pagos, Stripe). Acumula a cada compra. */
    @Column(name = "creditos_leads", nullable = false)
    private int creditosLeads = 0;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
