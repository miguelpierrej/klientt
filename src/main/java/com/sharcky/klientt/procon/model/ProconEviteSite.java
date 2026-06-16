package com.sharcky.klientt.procon.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Domínio constante na lista Procon-SP "Evite Sites" (ARQUITETURA §5).
 * Sincronizado periodicamente; comparado por domínio no cálculo do score.
 */
@Entity
@Table(name = "procon_evite_sites")
@Getter
@Setter
public class ProconEviteSite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String dominio;

    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(length = 14)
    private String cnpj;

    @Column(name = "sincronizado_em", nullable = false)
    private LocalDateTime sincronizadoEm;
}
