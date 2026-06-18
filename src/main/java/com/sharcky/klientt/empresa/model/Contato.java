package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Meio de contacto de uma empresa. Vários por empresa, com confiança ({@code verificado}).
 */
@Entity
@Table(name = "contatos")
@Getter
@Setter
public class Contato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 20)
    private String tipo;          // telefone | whatsapp | email | instagram | ...

    @Column(nullable = false, length = 255)
    private String valor;

    @Column(nullable = false)
    private boolean principal;

    @Column(nullable = false)
    private boolean verificado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
