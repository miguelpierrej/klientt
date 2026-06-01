package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Empresa/lead. Funciona como cache partilhado entre buscas (ARQUITETURA §5):
 * uma empresa é coletada uma vez e reutilizada por várias buscas de utilizadores.
 */
@Entity
@Table(name = "empresas")
@Getter
@Setter
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String nome;

    @Column(length = 14)
    private String cnpj;

    @Column(length = 30)
    private String telefone;

    @Column(length = 255)
    private String endereco;

    @Column(length = 120)
    private String cidade;

    @Column(length = 255)
    private String website;

    private Double lat;
    private Double lng;

    @Column(length = 50)
    private String fonte;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToOne(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    private Sinais sinais;

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EmpresaRede> redes = new ArrayList<>();

    public void definirSinais(Sinais s) {
        s.setEmpresa(this);
        this.sinais = s;
    }

    public void adicionarRede(EmpresaRede rede) {
        rede.setEmpresa(this);
        this.redes.add(rede);
    }
}
