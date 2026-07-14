package com.sharcky.klientt.busca.job;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface JobResultadoRepository extends JpaRepository<JobResultado, JobResultadoId> {

    List<JobResultado> findByJobId(Long jobId);

    /** Leads consumidos por um utilizador desde uma data (para a quota do plano). */
    @Query("""
            select count(jr) from JobResultado jr, JobBusca j
            where jr.jobId = j.id and j.utilizadorId = :utilizadorId and j.criadoEm >= :desde
            """)
    long contarLeadsDoUtilizadorDesde(@Param("utilizadorId") Long utilizadorId,
                                      @Param("desde") LocalDateTime desde);

    /** Nº de leads por cada job do utilizador — base do consumo de créditos (1ª página é grátis). */
    @Query("""
            select count(jr) from JobResultado jr, JobBusca j
            where jr.jobId = j.id and j.utilizadorId = :utilizadorId group by j.id
            """)
    List<Long> contarLeadsPorJobDoUtilizador(@Param("utilizadorId") Long utilizadorId);
}
