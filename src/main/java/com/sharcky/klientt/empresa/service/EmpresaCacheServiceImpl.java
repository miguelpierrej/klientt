package com.sharcky.klientt.empresa.service;

import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.EmpresaSocio;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Cache de empresas com identidade por CNPJ e merge campo-a-campo.
 *
 * <p>Identidade: CNPJ (só dígitos) quando existe; fallback nome+cidade. O merge é
 * <b>não-destrutivo</b>: um valor nulo nunca apaga o que já existia; quando ambos têm valor, o
 * fresco (recoleta) prevalece. Os contactos (telefone/email) são unidos por (tipo, valor).
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
        // Quando a fresca já traz contactos (mapeados da descoberta), mantém-nos; senão deriva do
        // telefone/email diretos (seed/legado).
        if (fresca.getContatos().isEmpty()) {
            derivarDeScalar(fresca).forEach(fresca::adicionarContato);
        }
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

        // Sinais de presença digital (Maps, via enriquecimento)
        alvo.setNota(coalesce(fresca.getNota(), alvo.getNota()));
        alvo.setAvaliacoes(coalesce(fresca.getAvaliacoes(), alvo.getAvaliacoes()));

        alvo.setAtualizadoEm(LocalDateTime.now());

        fundirContatos(alvo, fresca);
        fundirRedes(alvo, fresca);
        fundirSocios(alvo, fresca);
    }

    /** União dos sócios da fresca nos do alvo, por 'nome' — sem duplicar. */
    private void fundirSocios(Empresa alvo, Empresa fresca) {
        Set<String> existentes = new HashSet<>();
        for (EmpresaSocio s : alvo.getSocios()) {
            existentes.add(s.getNome() == null ? "" : s.getNome().toLowerCase());
        }
        for (EmpresaSocio novo : new ArrayList<>(fresca.getSocios())) {
            String chave = novo.getNome() == null ? "" : novo.getNome().toLowerCase();
            if (existentes.add(chave)) {
                EmpresaSocio copia = new EmpresaSocio();
                copia.setNome(novo.getNome());
                copia.setQualificacao(novo.getQualificacao());
                copia.setFaixaEtaria(novo.getFaixaEtaria());
                copia.setDesde(novo.getDesde());
                alvo.adicionarSocio(copia);
            }
        }
    }

    /** União das redes da fresca nas do alvo, por 'rede' (uma por rede) — sem duplicar. */
    private void fundirRedes(Empresa alvo, Empresa fresca) {
        Set<String> existentes = new HashSet<>();
        for (EmpresaRede r : alvo.getRedes()) {
            existentes.add(r.getRede() == null ? "" : r.getRede().toLowerCase());
        }
        for (EmpresaRede nova : new ArrayList<>(fresca.getRedes())) {
            String chave = nova.getRede() == null ? "" : nova.getRede().toLowerCase();
            if (existentes.add(chave)) {
                EmpresaRede copia = new EmpresaRede();
                copia.setRede(nova.getRede());
                copia.setUrl(nova.getUrl());
                alvo.adicionarRede(copia);
            }
        }
    }

    /** União dos contatos da fresca nos do alvo, por (tipo, valor) — sem duplicar. */
    private void fundirContatos(Empresa alvo, Empresa fresca) {
        Set<String> existentes = new HashSet<>();
        for (Contato c : alvo.getContatos()) {
            existentes.add(chaveContato(c.getTipo(), c.getValor()));
        }
        for (Contato novo : contatosDaFresca(fresca)) {
            if (existentes.add(chaveContato(novo.getTipo(), novo.getValor()))) {
                alvo.adicionarContato(novo);
            }
        }
    }

    /** Contactos a fundir: os já mapeados na fresca; senão deriva do telefone/email diretos. */
    private static List<Contato> contatosDaFresca(Empresa fresca) {
        if (!fresca.getContatos().isEmpty()) {
            return new ArrayList<>(fresca.getContatos());
        }
        return derivarDeScalar(fresca);
    }

    /** Deriva contatos diretos (telefone/email) dos campos diretos da empresa (seed/legado). */
    private static List<Contato> derivarDeScalar(Empresa e) {
        List<Contato> lista = new ArrayList<>();
        adicionar(lista, "telefone", e.getTelefone());
        adicionar(lista, "email", e.getEmail());
        return lista;
    }

    private static void adicionar(List<Contato> lista, String tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        Contato c = new Contato();
        c.setTipo(tipo);
        c.setValor(valor.trim());
        lista.add(c);
    }

    private static String chaveContato(String tipo, String valor) {
        return (tipo == null ? "" : tipo.toLowerCase()) + "|"
                + (valor == null ? "" : valor.trim().toLowerCase());
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
