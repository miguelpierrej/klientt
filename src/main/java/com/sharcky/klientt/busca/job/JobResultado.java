package com.sharcky.klientt.busca.job;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Liga um job às empresas encontradas (ARQUITETURA §5).
 * Referencia a empresa por id (sem relação JPA) para manter as features desacopladas.
 */
@Entity
@Table(name = "job_resultados")
@IdClass(JobResultadoId.class)
@Getter
@Setter
public class JobResultado {

    @Id
    @Column(name = "job_id")
    private Long jobId;

    @Id
    @Column(name = "empresa_id")
    private Long empresaId;

    public JobResultado() {
    }

    public JobResultado(Long jobId, Long empresaId) {
        this.jobId = jobId;
        this.empresaId = empresaId;
    }
}
