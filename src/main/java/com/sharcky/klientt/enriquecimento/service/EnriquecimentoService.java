package com.sharcky.klientt.enriquecimento.service;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.busca.mapper.ScrapeMapper;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.repository.EmpresaRepository;
import com.sharcky.klientt.enriquecimento.dto.EnriquecimentoCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Aplica o enriquecimento Maps a uma empresa (por CNPJ) e regista o enriquecimento no job.
 * A descoberta (Casa dos Dados) não traz sinais Google/site nem redes — é o Maps que os acrescenta;
 * o endereço do Maps é guardado e comparado com o cadastral (flag de divergência).
 */
@Service
public class EnriquecimentoService {

    private static final Logger log = LoggerFactory.getLogger(EnriquecimentoService.class);

    private final EmpresaRepository empresaRepository;
    private final ScrapeMapper scrapeMapper;
    private final JobService jobService;

    public EnriquecimentoService(EmpresaRepository empresaRepository, ScrapeMapper scrapeMapper,
                                 JobService jobService) {
        this.empresaRepository = empresaRepository;
        this.scrapeMapper = scrapeMapper;
        this.jobService = jobService;
    }

    @Transactional
    public void aplicar(EnriquecimentoCallback callback) {
        String cnpj = soDigitos(callback.cnpj());
        if (cnpj != null) {
            empresaRepository.findFirstByCnpj(cnpj).ifPresent(empresa -> aplicarNaEmpresa(empresa, callback));
        }
        Long jobId = parseJobId(callback.buscaId());
        if (jobId != null) {
            jobService.registarEnriquecimento(jobId);
        }
    }

    private void aplicarNaEmpresa(Empresa empresa, EnriquecimentoCallback callback) {
        empresa.setConfirmadoMaps(callback.encontrado());
        if (callback.encontrado()) {
            if (callback.sinais() != null) {
                empresa.definirSinais(scrapeMapper.toSinais(callback.sinais()));
            }
            if (callback.redes() != null && !callback.redes().isEmpty()) {
                // A descoberta não trouxe redes; o Maps é a fonte das redes.
                empresa.getRedes().clear();
                callback.redes().forEach(r -> empresa.adicionarRede(scrapeMapper.toRede(r)));
            }
            empresa.setEnderecoMaps(callback.enderecoMaps());
            empresa.setEnderecoDivergente(divergente(empresa.getEndereco(), callback.enderecoMaps()));
        }
        empresaRepository.save(empresa);
        log.info("Enriquecimento aplicado cnpj={} (confirmado={}, divergente={})",
                empresa.getCnpj(), empresa.getConfirmadoMaps(), empresa.getEnderecoDivergente());
    }

    /**
     * Heurística de divergência: dois endereços partilham &lt; 2 palavras significativas (≥3 letras)
     * → divergentes. Devolve {@code null} quando falta um dos endereços.
     */
    static Boolean divergente(String cadastral, String maps) {
        if (cadastral == null || cadastral.isBlank() || maps == null || maps.isBlank()) {
            return null;
        }
        Set<String> a = palavras(cadastral);
        Set<String> b = palavras(maps);
        a.retainAll(b);
        return a.size() < 2;
    }

    private static Set<String> palavras(String s) {
        String n = Normalizer.normalize(s.toLowerCase(), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        return Arrays.stream(n.split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 3)
                .collect(Collectors.toSet());
    }

    private static String soDigitos(String cnpj) {
        if (cnpj == null) {
            return null;
        }
        String d = cnpj.replaceAll("\\D", "");
        return d.isEmpty() ? null : d;
    }

    private Long parseJobId(String buscaId) {
        try {
            return Long.valueOf(buscaId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
