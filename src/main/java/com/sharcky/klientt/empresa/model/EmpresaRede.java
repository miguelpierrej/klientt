package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Perfil de rede social de uma empresa (ARQUITETURA §5).
 */
@Entity
@Table(name = "empresa_redes")
@Getter
@Setter
public class EmpresaRede {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @Column(nullable = false, length = 40)
    private String rede;          // instagram | facebook | linkedin | ...

    @Column(length = 255)
    private String url;

    private Integer seguidores;
}
