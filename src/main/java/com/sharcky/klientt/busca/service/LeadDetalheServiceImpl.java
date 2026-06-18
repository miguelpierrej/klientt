package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.mapper.LeadDetalheMapper;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadDetalheServiceImpl implements LeadDetalheService {

    private final EmpresaRepository empresaRepository;
    private final LeadDetalheMapper detalheMapper;

    public LeadDetalheServiceImpl(EmpresaRepository empresaRepository, LeadDetalheMapper detalheMapper) {
        this.empresaRepository = empresaRepository;
        this.detalheMapper = detalheMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public LeadDetalhe detalhe(Long empresaId) {
        Empresa empresa = empresaRepository.findById(empresaId)
                .orElseThrow(() -> new IllegalArgumentException("Empresa não encontrada: " + empresaId));
        return detalheMapper.toDetalhe(empresa);
    }
}
