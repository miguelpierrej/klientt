package com.sharcky.klientt.busca.web;

import com.sharcky.klientt.busca.dto.FiltroBusca;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.OrdenarPor;
import com.sharcky.klientt.busca.service.BuscaService;
import com.sharcky.klientt.conta.seguranca.KlienttUserDetails;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
public class ExportController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

    private final BuscaService buscaService;
    private final LeadCsvWriter csvWriter;

    public ExportController(BuscaService buscaService, LeadCsvWriter csvWriter) {
        this.buscaService = buscaService;
        this.csvWriter = csvWriter;
    }

    /** Exporta os leads do job (com os mesmos filtros/ordenação) em CSV. */
    @GetMapping("/buscar/{jobId}/exportar")
    public ResponseEntity<byte[]> exportar(@PathVariable Long jobId,
                                           @RequestParam(required = false) OrdenarPor ordenar,
                                           @RequestParam(defaultValue = "false") boolean semSite,
                                           @RequestParam(defaultValue = "false") boolean notaBaixa,
                                           @RequestParam(defaultValue = "false") boolean poucosSeguidores,
                                           @RequestParam(defaultValue = "false") boolean procon,
                                           @RequestParam(defaultValue = "false") boolean comContato,
                                           @AuthenticationPrincipal KlienttUserDetails utilizador) {
        FiltroBusca filtro = new FiltroBusca(ordenar, semSite, notaBaixa, poucosSeguidores, procon, comContato);
        List<LeadDetalhe> leads = buscaService.exportar(jobId, utilizador.getId(), filtro);
        byte[] csv = csvWriter.escrever(leads);

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename("leads-" + jobId + ".csv").build();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .contentType(CSV)
                .body(csv);
    }
}
