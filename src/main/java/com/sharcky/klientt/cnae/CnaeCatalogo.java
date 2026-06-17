package com.sharcky.klientt.cnae;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Subclasse CNAE do catálogo oficial do IBGE (código de 7 dígitos + descrição).
 */
@Entity
@Table(name = "cnae")
@Getter
@Setter
public class CnaeCatalogo {

    @Id
    @Column(length = 7)
    private String codigo;

    @Column(nullable = false, length = 255)
    private String descricao;
}
