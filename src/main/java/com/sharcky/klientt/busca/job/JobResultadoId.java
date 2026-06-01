package com.sharcky.klientt.busca.job;

import java.io.Serializable;
import java.util.Objects;

/** Chave composta de {@link JobResultado} (job_id + empresa_id). */
public class JobResultadoId implements Serializable {

    private Long jobId;
    private Long empresaId;

    public JobResultadoId() {
    }

    public JobResultadoId(Long jobId, Long empresaId) {
        this.jobId = jobId;
        this.empresaId = empresaId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JobResultadoId that)) return false;
        return Objects.equals(jobId, that.jobId) && Objects.equals(empresaId, that.empresaId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobId, empresaId);
    }
}
