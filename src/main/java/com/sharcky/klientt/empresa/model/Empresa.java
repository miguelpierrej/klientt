package com.sharcky.klientt.empresa.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
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
    private String email;

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

    // --- Dados cadastrais (CNPJ / Receita) ---
    @Column(name = "razao_social", length = 255)
    private String razaoSocial;

    @Column(name = "nome_fantasia", length = 255)
    private String nomeFantasia;

    @Column(name = "situacao_cadastral", length = 60)
    private String situacaoCadastral;

    @Column(name = "data_abertura")
    private LocalDate dataAbertura;

    @Column(name = "capital_social", precision = 15, scale = 2)
    private BigDecimal capitalSocial;

    @Column(length = 60)
    private String porte;

    @Column(name = "natureza_juridica", length = 255)
    private String naturezaJuridica;

    @Column(name = "cnae_principal", length = 255)
    private String cnaePrincipal;

    @Column(name = "optante_simples")
    private Boolean optanteSimples;

    @Column(name = "optante_mei")
    private Boolean optanteMei;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    @OneToMany(mappedBy = "empresa", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Contato> contatos = new ArrayList<>();

    public void adicionarContato(Contato contato) {
        contato.setEmpresa(this);
        this.contatos.add(contato);
    }

    /** Tem pelo menos um canal de contacto direto (telefone/whatsapp/email)? */
    public boolean isContactavel() {
        return contatos.stream().anyMatch(c ->
                "telefone".equalsIgnoreCase(c.getTipo())
                        || "whatsapp".equalsIgnoreCase(c.getTipo())
                        || "email".equalsIgnoreCase(c.getTipo()));
    }
}
