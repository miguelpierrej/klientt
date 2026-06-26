package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.mapper.EmpresaPayloadMapper;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Implementação da ingestão partilhada (ver {@link IngestaoService}): faz upsert na cache e liga
 * cada empresa ao job.
 */
@Service
public class IngestaoServiceImpl implements IngestaoService {

    private final EmpresaCacheService cacheService;
    private final EmpresaPayloadMapper payloadMapper;
    private final JobService jobService;

    public IngestaoServiceImpl(EmpresaCacheService cacheService, EmpresaPayloadMapper payloadMapper,
                               JobService jobService) {
        this.cacheService = cacheService;
        this.payloadMapper = payloadMapper;
        this.jobService = jobService;
    }

    @Override
    @Transactional
    public void ingerir(List<EmpresaPayload> empresas, Long jobId) {
        if (empresas == null) {
            return;
        }
        for (EmpresaPayload payload : empresas) {
            Empresa persistida = cacheService.upsert(payloadMapper.toEmpresa(payload));
            if (jobId != null) {
                jobService.registarResultado(jobId, persistida.getId());
            }
        }
    }
}
