package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.DescobertaProperties;
import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
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
        return descoberta.usaCasaDosDados()
                ? casaDosDados.buscarPorCnae(cnae, municipio, limite)
                : minhaReceita.buscarPorCnae(cnae, municipio, limite);
    }

    @Override
    public List<EmpresaPayload> buscarPorNome(String nome, String municipio, int limite) {
        return casaDosDados.buscarPorNome(nome, municipio, limite);
    }

    @Override
    public Pagina buscarPaginaPorCnae(String cnae, String municipio, String cursor, int tamanho) {
        return descoberta.usaCasaDosDados()
                ? casaDosDados.buscarPaginaPorCnae(cnae, municipio, cursor, tamanho)
                : minhaReceita.buscarPaginaPorCnae(cnae, municipio, cursor, tamanho);
    }
}
