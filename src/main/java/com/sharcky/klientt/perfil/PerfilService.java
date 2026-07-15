package com.sharcky.klientt.perfil;

import com.sharcky.klientt.busca.dto.SugestaoCnae;
import com.sharcky.klientt.cnae.CnaeCatalogoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Perfil do cliente (ICP): leitura, gravação pelo onboarding e "pular". Um por utilizador.
 */
@Service
public class PerfilService {

    private final PerfilClienteRepository repository;
    private final CnaeCatalogoRepository cnaeRepo;

    public PerfilService(PerfilClienteRepository repository, CnaeCatalogoRepository cnaeRepo) {
        this.repository = repository;
        this.cnaeRepo = cnaeRepo;
    }

    @Transactional(readOnly = true)
    public Optional<PerfilCliente> obter(Long utilizadorId) {
        return repository.findById(utilizadorId);
    }

    /** Nichos-alvo do perfil como sugestões (código + descrição) — para chips/atalhos. */
    @Transactional(readOnly = true)
    public List<SugestaoCnae> nichosDetalhados(PerfilCliente perfil) {
        if (perfil == null) {
            return List.of();
        }
        return perfil.nichos().stream()
                .map(cod -> cnaeRepo.findById(cod)
                        .map(c -> SugestaoCnae.de(c.getCodigo(), c.getDescricao()))
                        .orElseGet(() -> SugestaoCnae.de(cod, "")))
                .toList();
    }

    /** O onboarding já foi preenchido ou pulado? (para decidir o redirecionamento no 1º acesso). */
    @Transactional(readOnly = true)
    public boolean concluido(Long utilizadorId) {
        return repository.findById(utilizadorId).map(PerfilCliente::isConcluido).orElse(false);
    }

    @Transactional
    public void salvar(Long utilizadorId, PerfilForm form) {
        PerfilCliente p = existenteOuNovo(utilizadorId);
        p.setOferta(vazioParaNull(form.getOferta()));
        p.setNichosAlvo(normalizarCsv(form.getNichosAlvo()));
        p.setRegioesAlvo(normalizarCsv(form.getRegioesAlvo()));
        p.setPortesAlvo(form.getPortes() == null || form.getPortes().isEmpty() ? null : String.join(",", form.getPortes()));
        p.setQuerSemSite(form.isQuerSemSite());
        p.setQuerSimplesMei(form.isQuerSimplesMei());
        p.setQuerComContato(form.isQuerComContato());
        p.setConcluido(true);
        p.setAtualizadoEm(LocalDateTime.now());
        repository.save(p);
    }

    /** Marca como concluído sem dados (o utilizador pulou o onboarding). */
    @Transactional
    public void pular(Long utilizadorId) {
        PerfilCliente p = existenteOuNovo(utilizadorId);
        p.setConcluido(true);
        p.setAtualizadoEm(LocalDateTime.now());
        repository.save(p);
    }

    private PerfilCliente existenteOuNovo(Long utilizadorId) {
        return repository.findById(utilizadorId).orElseGet(() -> {
            PerfilCliente novo = new PerfilCliente();
            novo.setUtilizadorId(utilizadorId);
            return novo;
        });
    }

    private static String vazioParaNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }

    /** Limpa uma CSV: remove espaços e itens vazios; null se não sobrar nada. */
    private static String normalizarCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return null;
        }
        List<String> itens = java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .toList();
        return itens.isEmpty() ? null : String.join(",", itens);
    }
}
