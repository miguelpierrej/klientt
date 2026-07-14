package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Rede social de uma empresa (Instagram/Facebook/…), recolhida no enriquecimento.
 * Uma por rede por empresa (UNIQUE empresa_id + rede).
 */
@Entity
@Table(name = "empresa_redes", uniqueConstraints = @UniqueConstraint(columnNames = {"empresa_id", "rede"}))
@Getter
@Setter
public class EmpresaRede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 30)
    private String rede;          // instagram | facebook | linkedin | youtube | tiktok | x | twitter

    @Column(nullable = false, length = 255)
    private String url;
}
