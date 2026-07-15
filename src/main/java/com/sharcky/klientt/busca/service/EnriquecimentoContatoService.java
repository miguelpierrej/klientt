package com.sharcky.klientt.busca.service;

import com.sharcky.klientt.cnpj.FonteContatoCnpj;
import com.sharcky.klientt.cnpj.config.ContatoFallbackProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.service.EmpresaCacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Enriquecimento de contato em BACKGROUND (fora do caminho crítico da busca). Para cada empresa com
 * CNPJ mas sem contato, consulta a cadeia pública (CNPJá open → BrasilAPI) e funde na cache.
 *
 * <p>Corre em {@code @Async}: o job da busca conclui logo na descoberta (resultados rápidos) e os
 * contatos em falta vão sendo preenchidos aqui — o throttle de 5 req/min do CNPJá deixa de travar o
 * utilizador. Os contatos aparecem no detalhe/lista à medida que a cache é atualizada.
 */
@Service
public class EnriquecimentoContatoService {

    private static final Logger log = LoggerFactory.getLogger(EnriquecimentoContatoService.class);

    private final ContatoFallbackProperties contatoFallback;
    private final FonteContatoCnpj fonteContato;
    private final EmpresaCacheService cacheService;

    public EnriquecimentoContatoService(ContatoFallbackProperties contatoFallback,
                                        FonteContatoCnpj fonteContato, EmpresaCacheService cacheService) {
        this.contatoFallback = contatoFallback;
        this.fonteContato = fonteContato;
        this.cacheService = cacheService;
    }

    @Async
    public void enriquecer(List<EmpresaPayload> empresas) {
        if (!contatoFallback.isEnabled() || empresas == null) {
            return;
        }
        int preenchidos = 0;
        for (EmpresaPayload e : empresas) {
            if (temContato(e) || e.cnpj() == null || e.cnpj().isBlank()) {
                continue;
            }
            try {
                FonteContatoCnpj.Contatos extra = fonteContato.consultar(e.cnpj());
                if (extra.isVazio()) {
                    continue;
                }
                cacheService.upsert(patchDeContatos(e, extra));
                preenchidos++;
            } catch (Exception ex) {
                // Uma falha num CNPJ não deve abortar o enriquecimento dos restantes.
                log.warn("Falha ao enriquecer contato (cnpj={}): {}", e.cnpj(), ex.getMessage());
            }
        }
        if (preenchidos > 0) {
            log.info("Enriquecimento de contato (background) preencheu {} de {} empresas", preenchidos, empresas.size());
        }
    }

    private static boolean temContato(EmpresaPayload e) {
        return (e.telefones() != null && !e.telefones().isEmpty())
                || (e.emails() != null && !e.emails().isEmpty());
    }

    /** Empresa mínima (identificada por CNPJ) só com os contatos do fallback, para o merge da cache. */
    private static Empresa patchDeContatos(EmpresaPayload e, FonteContatoCnpj.Contatos extra) {
        Empresa patch = new Empresa();
        patch.setNome(e.nome());
        patch.setCidade(e.cidade());
        patch.setCnpj(e.cnpj());
        extra.telefones().forEach(t -> patch.adicionarContato(contato("telefone", t)));
        extra.emails().forEach(m -> patch.adicionarContato(contato("email", m)));
        if (!extra.telefones().isEmpty()) {
            patch.setTelefone(extra.telefones().get(0));
        }
        if (!extra.emails().isEmpty()) {
            patch.setEmail(extra.emails().get(0));
        }
        return patch;
    }

    private static Contato contato(String tipo, String valor) {
        Contato c = new Contato();
        c.setTipo(tipo);
        c.setValor(valor);
        return c;
    }
}
