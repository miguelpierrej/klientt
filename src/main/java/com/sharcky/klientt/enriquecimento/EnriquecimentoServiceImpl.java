package com.sharcky.klientt.enriquecimento;

import com.sharcky.klientt.busca.job.JobService;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import com.sharcky.klientt.enriquecimento.dto.EnrichCallback;
import com.sharcky.klientt.enriquecimento.dto.EnrichCallback.EmpresaEnriquecida;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Funde as empresas enriquecidas pelo scraper na cache (por CNPJ) e, nos estados terminais
 * (CONCLUIDO/ERRO), conclui o job. Cada lote (PARCIAL) é aplicado à medida que chega.
 */
@Service
public class EnriquecimentoServiceImpl implements EnriquecimentoService {

    private static final Logger log = LoggerFactory.getLogger(EnriquecimentoServiceImpl.class);

    private final EmpresaCacheService cacheService;
    private final JobService jobService;

    public EnriquecimentoServiceImpl(EmpresaCacheService cacheService, JobService jobService) {
        this.cacheService = cacheService;
        this.jobService = jobService;
    }

    @Override
    @Transactional
    public void aplicar(EnrichCallback callback) {
        Long jobId = parseJobId(callback.buscaId());
        // Callback obsoleto/desconhecido (ex.: app reiniciado → H2 em memória perdeu os jobs): ignora
        // graciosamente. Sem isto, o registarResultado dá violação de FK (job_resultados → jobs_busca).
        if (jobId == null || jobService.obter(jobId).isEmpty()) {
            log.warn("Callback de enriquecimento para job inexistente (buscaId={}) — ignorado", callback.buscaId());
            return;
        }
        int aplicadas = 0;
        for (EmpresaEnriquecida e : nullSafe(callback.empresas())) {
            Empresa persistida = cacheService.upsert(toEmpresa(e));
            jobService.registarResultado(jobId, persistida.getId());
            aplicadas++;
        }
        if (aplicadas > 0) {
            log.info("Enriquecimento aplicado: job={} estado={} empresas={}",
                    callback.buscaId(), callback.estado(), aplicadas);
        }
        if (terminal(callback.estado())) {
            jobService.concluir(jobId);
        }
    }

    /** Constrói uma {@link Empresa} "patch" (identificada por CNPJ) para o merge da cache. */
    private static Empresa toEmpresa(EmpresaEnriquecida e) {
        Empresa emp = new Empresa();
        emp.setCnpj(e.cnpj());
        emp.setNome(coalesce(e.razaoSocial(), e.nomeFantasia(), e.cnpj(), "Empresa"));
        emp.setNomeFantasia(e.nomeFantasia());
        emp.setRazaoSocial(e.razaoSocial());
        emp.setWebsite(e.website());

        aplicarEndereco(emp, e.endereco());
        aplicarMaps(emp, e.googleMaps());
        aplicarCadastrais(emp, e.dadosCadastrais());
        aplicarContatos(emp, e);
        aplicarRedes(emp, e.redes());
        return emp;
    }

    private static void aplicarEndereco(Empresa emp, EnrichCallback.Endereco end) {
        if (end == null) {
            return;
        }
        emp.setCidade(end.cidade());
        String linha = String.join(", ",
                nullSafeStr(end.logradouro()) + (end.numero() != null ? " " + end.numero() : ""),
                nullSafeStr(end.bairro())).replaceAll("(^,\\s*)|(,\\s*$)", "").trim();
        if (!linha.isBlank()) {
            emp.setEndereco(linha);
        }
    }

    private static void aplicarMaps(Empresa emp, EnrichCallback.GoogleMaps maps) {
        if (maps == null) {
            return;
        }
        emp.setNota(maps.nota());
        emp.setAvaliacoes(maps.avaliacoes());
    }

    private static void aplicarCadastrais(Empresa emp, EnrichCallback.Cadastrais cad) {
        if (cad == null) {
            return;
        }
        emp.setCnaePrincipal(cad.cnae());
        emp.setSituacaoCadastral(cad.situacaoCadastral());
        emp.setPorte(cad.porte());
        emp.setCapitalSocial(cad.capitalSocial());
        emp.setNaturezaJuridica(cad.naturezaJuridica());
        emp.setDataAbertura(parseData(cad.dataAbertura()));
    }

    private static void aplicarContatos(Empresa emp, EmpresaEnriquecida e) {
        for (EnrichCallback.Email m : nullSafe(e.emails())) {
            adicionarContato(emp, "email", m.email());
        }
        for (EnrichCallback.Telefone t : nullSafe(e.telefones())) {
            adicionarContato(emp, "telefone", t.telefone());
        }
        emp.getContatos().stream().filter(c -> "telefone".equals(c.getTipo())).findFirst()
                .ifPresent(c -> emp.setTelefone(c.getValor()));
        emp.getContatos().stream().filter(c -> "email".equals(c.getTipo())).findFirst()
                .ifPresent(c -> emp.setEmail(c.getValor()));
    }

    private static void adicionarContato(Empresa emp, String tipo, String valor) {
        if (valor == null || valor.isBlank()) {
            return;
        }
        Contato c = new Contato();
        c.setTipo(tipo);
        c.setValor(valor.trim());
        emp.adicionarContato(c);
    }

    private static void aplicarRedes(Empresa emp, List<EnrichCallback.Rede> redes) {
        for (EnrichCallback.Rede r : nullSafe(redes)) {
            if (r.rede() == null || r.url() == null || r.url().isBlank()) {
                continue;
            }
            EmpresaRede rede = new EmpresaRede();
            rede.setRede(r.rede());
            rede.setUrl(r.url().trim());
            emp.adicionarRede(rede);
        }
    }

    private static LocalDate parseData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(valor.substring(0, Math.min(10, valor.length())));
        } catch (DateTimeParseException | IndexOutOfBoundsException ex) {
            return null;
        }
    }

    private static boolean terminal(String estado) {
        return "CONCLUIDO".equalsIgnoreCase(estado) || "ERRO".equalsIgnoreCase(estado);
    }

    private static Long parseJobId(String buscaId) {
        try {
            return buscaId == null ? null : Long.valueOf(buscaId);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    @SafeVarargs
    private static <T> T coalesce(T... valores) {
        for (T v : valores) {
            if (v != null && !(v instanceof String s && s.isBlank())) {
                return v;
            }
        }
        return null;
    }

    private static <T> List<T> nullSafe(List<T> lista) {
        return lista == null ? List.of() : lista;
    }

    private static String nullSafeStr(String s) {
        return s == null ? "" : s;
    }
}
