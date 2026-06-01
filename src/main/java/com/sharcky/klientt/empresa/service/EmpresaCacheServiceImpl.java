package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EmpresaCacheServiceImpl implements EmpresaCacheService {

    private final EmpresaRepository empresaRepository;

    public EmpresaCacheServiceImpl(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional
    public Empresa upsert(Empresa fresca) {
        Optional<Empresa> existente = empresaRepository
                .findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(fresca.getNome(), nullParaVazio(fresca.getCidade()));

        if (existente.isPresent()) {
            Empresa alvo = existente.get();
            atualizar(alvo, fresca);
            return empresaRepository.save(alvo);
        }
        fresca.setAtualizadoEm(LocalDateTime.now());
        return empresaRepository.save(fresca);
    }

    /** Atualiza a empresa em cache com dados frescos (sinais e redes substituídos). */
    private void atualizar(Empresa alvo, Empresa fresca) {
        alvo.setCnpj(fresca.getCnpj());
        alvo.setTelefone(fresca.getTelefone());
        alvo.setEndereco(fresca.getEndereco());
        alvo.setWebsite(fresca.getWebsite());
        alvo.setLat(fresca.getLat());
        alvo.setLng(fresca.getLng());
        alvo.setFonte(fresca.getFonte());
        alvo.setAtualizadoEm(LocalDateTime.now());

        if (fresca.getSinais() != null) {
            alvo.definirSinais(fresca.getSinais());
        }
        alvo.getRedes().clear();
        fresca.getRedes().forEach(alvo::adicionarRede);
    }

    private String nullParaVazio(String s) {
        return s == null ? "" : s;
    }
}
