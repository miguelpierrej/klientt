package com.sharcky.klientt.pagamento.repository;

import com.sharcky.klientt.pagamento.model.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {

    boolean existsByStripeSessionId(String stripeSessionId);

    List<Pagamento> findByUtilizadorIdOrderByCriadoEmDesc(Long utilizadorId);
}
