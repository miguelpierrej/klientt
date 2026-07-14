package com.sharcky.klientt.pagamento.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Compra de créditos via Stripe. {@code stripeSessionId} é único → o webhook credita uma só vez
 * (idempotência contra reentregas). Serve também de histórico de compras.
 */
@Entity
@Table(name = "pagamentos")
@Getter
@Setter
public class Pagamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "utilizador_id", nullable = false)
    private Long utilizadorId;

    @Column(name = "stripe_session_id", nullable = false, length = 255, unique = true)
    private String stripeSessionId;

    @Column(nullable = false)
    private int leads;

    @Column(name = "valor_centavos")
    private Integer valorCentavos;

    @Column(length = 10)
    private String moeda;

    @Column(length = 30)
    private String estado;

    @Column(name = "criado_em", nullable = false)
    private LocalDateTime criadoEm = LocalDateTime.now();
}
