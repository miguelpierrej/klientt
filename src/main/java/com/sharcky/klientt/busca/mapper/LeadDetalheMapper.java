package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.RedeView;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converte (Empresa + AvaliacaoLead) na vista completa {@link LeadDetalhe}.
 * Achata os sinais e as redes; o score vem da avaliação.
 */
@Mapper(componentModel = "spring")
public interface LeadDetalheMapper {

    @Mapping(target = "id", source = "empresa.id")
    @Mapping(target = "nome", source = "empresa.nome")
    @Mapping(target = "cidade", source = "empresa.cidade")
    @Mapping(target = "cnpj", source = "empresa.cnpj")
    @Mapping(target = "telefone", source = "empresa.telefone")
    @Mapping(target = "endereco", source = "empresa.endereco")
    @Mapping(target = "website", source = "empresa.website")
    @Mapping(target = "fonte", source = "empresa.fonte")
    @Mapping(target = "atualizadoEm", source = "empresa.atualizadoEm")
    @Mapping(target = "score", source = "avaliacao.score")
    @Mapping(target = "notaGoogle", source = "empresa.sinais.notaGoogle")
    @Mapping(target = "numReviews", source = "empresa.sinais.numReviews")
    @Mapping(target = "siteExiste", source = "empresa.sinais.siteExiste")
    @Mapping(target = "siteVelocidadeMs", source = "empresa.sinais.siteVelocidadeMs")
    @Mapping(target = "siteHttps", source = "empresa.sinais.siteHttps")
    @Mapping(target = "siteNumPaginas", source = "empresa.sinais.siteNumPaginas")
    @Mapping(target = "siteReputacao", source = "empresa.sinais.siteReputacao")
    @Mapping(target = "proconEviteSite", source = "empresa.sinais.proconEviteSite")
    @Mapping(target = "redes", source = "empresa.redes")
    LeadDetalhe toDetalhe(Empresa empresa, AvaliacaoLead avaliacao);

    RedeView toRedeView(EmpresaRede rede);
}
