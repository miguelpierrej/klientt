package com.sharcky.klientt.conta.service;

import com.sharcky.klientt.conta.dto.ResumoConta;
import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContaServiceImpl implements ContaService {

    private final UtilizadorRepository utilizadorRepository;
    private final CreditosService creditosService;

    public ContaServiceImpl(UtilizadorRepository utilizadorRepository, CreditosService creditosService) {
        this.utilizadorRepository = utilizadorRepository;
        this.creditosService = creditosService;
    }

    @Override
    @Transactional(readOnly = true)
    public ResumoConta resumo(Long utilizadorId) {
        Utilizador u = utilizadorRepository.findById(utilizadorId)
                .orElseThrow(() -> new IllegalArgumentException("Utilizador não encontrado: " + utilizadorId));

        return new ResumoConta(
                u.getNome(),
                u.getEmail(),
                creditosService.comprado(utilizadorId),
                creditosService.consumido(utilizadorId),
                creditosService.disponivel(utilizadorId));
    }
}
