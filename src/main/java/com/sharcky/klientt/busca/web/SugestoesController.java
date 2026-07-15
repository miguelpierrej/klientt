package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.SugestaoCnae;
import com.sharcky.klientt.cnae.CnaeCatalogo;
import com.sharcky.klientt.cnae.CnaeCatalogoRepository;
import com.sharcky.klientt.cnae.SinonimoCnae;
import com.sharcky.klientt.cnpj.ResolvedorMunicipio;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * Autocomplete do formulário de busca: sugere atividades (CNAE, por descrição) e cidades
 * (municipios.csv). Devolve fragmentos HTML consumidos por HTMX.
 */
@Controller
public class SugestoesController {

    private static final int LIMITE = 8;

    private final CnaeCatalogoRepository cnaeRepo;
    private final SinonimoCnae sinonimoCnae;
    private final ResolvedorMunicipio municipios;

    public SugestoesController(CnaeCatalogoRepository cnaeRepo, SinonimoCnae sinonimoCnae,
                              ResolvedorMunicipio municipios) {
        this.cnaeRepo = cnaeRepo;
        this.sinonimoCnae = sinonimoCnae;
        this.municipios = municipios;
    }

    /**
     * Sugestões de atividade (CNAE): primeiro os sinónimos coloquiais (ex.: "dentista" → odontológica),
     * depois a busca por descrição oficial — deduplicado e limitado.
     */
    @GetMapping("/sugestoes/cnae")
    public String cnae(@RequestParam(name = "termo", required = false) String q, Model model) {
        model.addAttribute("sugestoes", sugestoesCnae(q));
        return "fragments/sugestoes :: cnae";
    }

    private List<SugestaoCnae> sugestoesCnae(String q) {
        if (q == null || q.trim().length() < 2) {
            return List.of();
        }
        LinkedHashMap<String, SugestaoCnae> out = new LinkedHashMap<>();
        // 1) sinónimos (cobrem termos que não estão na descrição oficial).
        for (String cod : sinonimoCnae.codigosPorTexto(q)) {
            cnaeRepo.findById(cod).ifPresent(c ->
                    out.putIfAbsent(c.getCodigo(), SugestaoCnae.de(c.getCodigo(), c.getDescricao())));
        }
        // 2) descrição oficial.
        for (CnaeCatalogo c : cnaeRepo.findTop8ByDescricaoContainingIgnoreCaseOrderByDescricaoAsc(q.trim())) {
            out.putIfAbsent(c.getCodigo(), SugestaoCnae.de(c.getCodigo(), c.getDescricao()));
        }
        return out.values().stream().limit(8).toList();
    }

    /** Sugestões de cidade (nome + UF) para o campo "Cidade / região". */
    @GetMapping("/sugestoes/cidade")
    public String cidade(@RequestParam(name = "regiao", required = false) String q, Model model) {
        model.addAttribute("sugestoes", municipios.sugerir(q, LIMITE));
        return "fragments/sugestoes :: cidade";
    }
}
