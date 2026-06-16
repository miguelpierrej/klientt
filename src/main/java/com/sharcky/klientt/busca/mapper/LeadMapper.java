package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.busca.scoring.AvaliacaoLead;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.Mapping;
import org.mapstruct.Mapper;

/**
 * Converte (Empresa + AvaliacaoLead) no DTO de resposta {@link LeadResponse}.
 * Apenas cópia de campos — sem regras de negócio (essas vivem no AvaliadorLead).
 */
@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "id", source = "empresa.id")
    @Mapping(target = "nome", source = "empresa.nome")
    @Mapping(target = "cidade", source = "empresa.cidade")
    @Mapping(target = "notaGoogle", source = "avaliacao.notaGoogle")
    @Mapping(target = "temSite", source = "avaliacao.temSite")
    @Mapping(target = "siteLento", source = "avaliacao.siteLento")
    @Mapping(target = "seguidores", source = "avaliacao.seguidores")
    @Mapping(target = "proconEviteSite", source = "avaliacao.proconEviteSite")
    @Mapping(target = "contactavel", source = "empresa.contactavel")
    @Mapping(target = "score", source = "avaliacao.score")
    LeadResponse toResponse(Empresa empresa, AvaliacaoLead avaliacao);
}
