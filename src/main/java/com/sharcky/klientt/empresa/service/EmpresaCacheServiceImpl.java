package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Cache de empresas com identidade por CNPJ e merge campo-a-campo (PLANO-DUAL-FONTE.md, Fase B).
 *
 * <p>Identidade: CNPJ (só dígitos) quando existe; fallback nome+cidade. Como as duas fontes
 * (Maps + CNPJ-por-CNAE) alimentam o mesmo lead, o merge é <b>não-destrutivo</b>: um valor nulo
 * de uma fonte nunca apaga o que a outra já trouxe (a Receita dá email/cadastrais, o Maps dá
 * nota/site/redes). Quando ambos têm valor, o fresco (recoleta) prevalece.
 */
@Service
public class EmpresaCacheServiceImpl implements EmpresaCacheService {

    private final EmpresaRepository empresaRepository;

    public EmpresaCacheServiceImpl(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    @Override
    @Transactional
    public Empresa upsert(Empresa fresca) {
        String cnpj = normalizarCnpj(fresca.getCnpj());

        // Identidade: CNPJ primeiro; fallback nome+cidade (apanha o mesmo lead cacheado antes sem CNPJ).
        Optional<Empresa> existente = Optional.empty();
        if (cnpj != null) {
            existente = empresaRepository.findFirstByCnpj(cnpj);
        }
        if (existente.isEmpty()) {
            existente = empresaRepository
                    .findFirstByNomeIgnoreCaseAndCidadeIgnoreCase(fresca.getNome(), nullParaVazio(fresca.getCidade()));
        }

        if (existente.isPresent()) {
            Empresa alvo = existente.get();
            fundir(alvo, fresca);
            return empresaRepository.save(alvo);
        }

        fresca.setCnpj(cnpj);
        fresca.setAtualizadoEm(LocalDateTime.now());
        return empresaRepository.save(fresca);
    }

    /** Funde os dados frescos no alvo sem apagar valores existentes (null não sobrescreve). */
    private void fundir(Empresa alvo, Empresa fresca) {
        alvo.setCnpj(coalesce(normalizarCnpj(fresca.getCnpj()), alvo.getCnpj()));
        alvo.setTelefone(coalesce(fresca.getTelefone(), alvo.getTelefone()));
        alvo.setEmail(coalesce(fresca.getEmail(), alvo.getEmail()));
        alvo.setEndereco(coalesce(fresca.getEndereco(), alvo.getEndereco()));
        alvo.setWebsite(coalesce(fresca.getWebsite(), alvo.getWebsite()));
        alvo.setLat(coalesce(fresca.getLat(), alvo.getLat()));
        alvo.setLng(coalesce(fresca.getLng(), alvo.getLng()));
        alvo.setFonte(coalesce(fresca.getFonte(), alvo.getFonte()));

        // Dados cadastrais (CNPJ / Receita)
        alvo.setRazaoSocial(coalesce(fresca.getRazaoSocial(), alvo.getRazaoSocial()));
        alvo.setNomeFantasia(coalesce(fresca.getNomeFantasia(), alvo.getNomeFantasia()));
        alvo.setSituacaoCadastral(coalesce(fresca.getSituacaoCadastral(), alvo.getSituacaoCadastral()));
        alvo.setDataAbertura(coalesce(fresca.getDataAbertura(), alvo.getDataAbertura()));
        alvo.setCapitalSocial(coalesce(fresca.getCapitalSocial(), alvo.getCapitalSocial()));
        alvo.setPorte(coalesce(fresca.getPorte(), alvo.getPorte()));
        alvo.setNaturezaJuridica(coalesce(fresca.getNaturezaJuridica(), alvo.getNaturezaJuridica()));
        alvo.setCnaePrincipal(coalesce(fresca.getCnaePrincipal(), alvo.getCnaePrincipal()));
        alvo.setOptanteSimples(coalesce(fresca.getOptanteSimples(), alvo.getOptanteSimples()));
        alvo.setOptanteMei(coalesce(fresca.getOptanteMei(), alvo.getOptanteMei()));

        alvo.setAtualizadoEm(LocalDateTime.now());

        fundirSinais(alvo, fresca.getSinais());
        fundirRedes(alvo, fresca);
    }

    /** Merge dos sinais: preenche nulos sem apagar; Procon é OR (qualquer fonte que flague mantém). */
    private void fundirSinais(Empresa alvo, Sinais fresca) {
        if (fresca == null) {
            return;
        }
        Sinais atual = alvo.getSinais();
        if (atual == null) {
            alvo.definirSinais(fresca);
            return;
        }
        atual.setNotaGoogle(coalesce(fresca.getNotaGoogle(), atual.getNotaGoogle()));
        atual.setNumReviews(coalesce(fresca.getNumReviews(), atual.getNumReviews()));
        atual.setSiteExiste(coalesce(fresca.getSiteExiste(), atual.getSiteExiste()));
        atual.setSiteVelocidadeMs(coalesce(fresca.getSiteVelocidadeMs(), atual.getSiteVelocidadeMs()));
        atual.setSiteHttps(coalesce(fresca.getSiteHttps(), atual.getSiteHttps()));
        atual.setSiteNumPaginas(coalesce(fresca.getSiteNumPaginas(), atual.getSiteNumPaginas()));
        atual.setSiteReputacao(coalesce(fresca.getSiteReputacao(), atual.getSiteReputacao()));
        atual.setProconEviteSite(atual.isProconEviteSite() || fresca.isProconEviteSite());
        atual.setColetadoEm(LocalDateTime.now());
    }

    /** União das redes por (rede, url); para uma rede já existente, atualiza seguidores se vierem. */
    private void fundirRedes(Empresa alvo, Empresa fresca) {
        Map<String, EmpresaRede> existentes = new HashMap<>();
        for (EmpresaRede r : alvo.getRedes()) {
            existentes.put(chaveRede(r), r);
        }
        for (EmpresaRede nova : fresca.getRedes()) {
            EmpresaRede atual = existentes.get(chaveRede(nova));
            if (atual == null) {
                alvo.adicionarRede(nova);
            } else if (nova.getSeguidores() != null) {
                atual.setSeguidores(nova.getSeguidores());
            }
        }
    }

    private static String chaveRede(EmpresaRede r) {
        String rede = r.getRede() == null ? "" : r.getRede().toLowerCase();
        String url = r.getUrl() == null ? "" : r.getUrl().toLowerCase();
        return rede + "|" + url;
    }

    private static <T> T coalesce(T fresco, T existente) {
        return fresco != null ? fresco : existente;
    }

    /** Mantém só os dígitos do CNPJ; devolve null se vazio (a máscara é descartada). */
    private static String normalizarCnpj(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        String digitos = cnpj.replaceAll("\\D", "");
        return digitos.isEmpty() ? null : digitos;
    }

    private static String nullParaVazio(String s) {
        return s == null ? "" : s;
    }
}
