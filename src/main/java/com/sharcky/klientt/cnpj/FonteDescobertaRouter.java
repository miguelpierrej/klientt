package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.DescobertaProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Encaminha a descoberta para a fonte certa (ver 'Nova integração.md', D3/D5):
 * <ul>
 *   <li><b>CNAE (nicho):</b> Minha Receita (grátis) por default; Casa dos Dados só quando
 *       {@code klientt.descoberta.fonte=casadosdados}.</li>
 *   <li><b>NOME:</b> sempre Casa dos Dados — o Minha Receita não faz busca textual.</li>
 * </ul>
 * É o bean {@code @Primary} de {@link FonteCnpj} (o {@code FonteCnpjExecutor} injeta este).
 */
@Service
@Primary
public class FonteDescobertaRouter implements FonteCnpj {

    private static final Logger log = LoggerFactory.getLogger(FonteDescobertaRouter.class);

    private final MinhaReceitaFonte minhaReceita;
    private final ApiGeridaCnpjFonte casaDosDados;
    private final DescobertaProperties descoberta;

    public FonteDescobertaRouter(MinhaReceitaFonte minhaReceita, ApiGeridaCnpjFonte casaDosDados,
                                 DescobertaProperties descoberta) {
        this.minhaReceita = minhaReceita;
        this.casaDosDados = casaDosDados;
        this.descoberta = descoberta;
    }

    @Override
    public List<EmpresaPayload> buscarPorCnae(String cnae, String municipio, int limite) {
        if (descoberta.usaCasaDosDados()) {
            return casaDosDados.buscarPorCnae(cnae, municipio, limite);
        }
        try {
            return minhaReceita.buscarPorCnae(cnae, municipio, limite);
        } catch (FonteDescobertaException ex) {
            if (descoberta.fallbackAtivo()) {
                log.info("Minha Receita indisponível (cnae={}) — fallback para Casa dos Dados: {}", cnae, ex.getMessage());
                return casaDosDados.buscarPorCnae(cnae, municipio, limite);
            }
            log.warn("Minha Receita indisponível (cnae={}), sem fallback: {}", cnae, ex.getMessage());
            return List.of();
        }
    }

    @Override
    public List<EmpresaPayload> buscarPorNome(String nome, String municipio, int limite) {
        return casaDosDados.buscarPorNome(nome, municipio, limite);
    }

    @Override
    public Pagina buscarPaginaPorCnae(String cnae, String municipio, String cursor, int tamanho) {
        if (descoberta.usaCasaDosDados()) {
            return casaDosDados.buscarPaginaPorCnae(cnae, municipio, cursor, tamanho);
        }
        try {
            // Sucesso (mesmo vazio = "sem resultados") NÃO aciona o fallback — só a falha real (exceção).
            return minhaReceita.buscarPaginaPorCnae(cnae, municipio, cursor, tamanho);
        } catch (FonteDescobertaException ex) {
            // Fallback só na PÁGINA INICIAL (cursor null): o cursor é de uma fonte específica, e a página
            // do fallback vem sem cursor, logo sem "carregar mais" (modo degradado).
            if (descoberta.fallbackAtivo() && cursor == null) {
                log.info("Minha Receita indisponível (cnae={}) — fallback para Casa dos Dados: {}", cnae, ex.getMessage());
                return casaDosDados.buscarPaginaPorCnae(cnae, municipio, null, tamanho);
            }
            log.warn("Minha Receita indisponível (cnae={}), sem fallback: {}", cnae, ex.getMessage());
            return new Pagina(List.of(), null);
        }
    }
}
