package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.ContatoView;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.RedeView;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.empresa.model.Contato;
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
    @Mapping(target = "email", source = "empresa.email")
    @Mapping(target = "endereco", source = "empresa.endereco")
    @Mapping(target = "enderecoMaps", source = "empresa.enderecoMaps")
    @Mapping(target = "enderecoDivergente", source = "empresa.enderecoDivergente")
    @Mapping(target = "website", source = "empresa.website")
    @Mapping(target = "fonte", source = "empresa.fonte")
    @Mapping(target = "contactavel", source = "empresa.contactavel")
    @Mapping(target = "contatos", source = "empresa.contatos")
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
    @Mapping(target = "razaoSocial", source = "empresa.razaoSocial")
    @Mapping(target = "nomeFantasia", source = "empresa.nomeFantasia")
    @Mapping(target = "situacaoCadastral", source = "empresa.situacaoCadastral")
    @Mapping(target = "dataAbertura", source = "empresa.dataAbertura")
    @Mapping(target = "capitalSocial", source = "empresa.capitalSocial")
    @Mapping(target = "porte", source = "empresa.porte")
    @Mapping(target = "naturezaJuridica", source = "empresa.naturezaJuridica")
    @Mapping(target = "cnaePrincipal", source = "empresa.cnaePrincipal")
    @Mapping(target = "optanteSimples", source = "empresa.optanteSimples")
    @Mapping(target = "optanteMei", source = "empresa.optanteMei")
    LeadDetalhe toDetalhe(Empresa empresa, AvaliacaoLead avaliacao);

    RedeView toRedeView(EmpresaRede rede);

    ContatoView toContatoView(Contato contato);
}
