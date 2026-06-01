package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.Sinais;
import com.sharcky.klientt.scraper.dto.EmpresaPayload;
import com.sharcky.klientt.scraper.dto.RedePayload;
import com.sharcky.klientt.scraper.dto.SinaisPayload;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Converte o payload do scraper nas entidades de domínio.
 * As referências inversas (empresa) são ligadas em @AfterMapping.
 */
@Mapper(componentModel = "spring")
public interface ScrapeMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    Empresa toEmpresa(EmpresaPayload payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "coletadoEm", ignore = true)
    Sinais toSinais(SinaisPayload payload);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    EmpresaRede toRede(RedePayload payload);

    @AfterMapping
    default void ligarReferencias(@MappingTarget Empresa empresa) {
        if (empresa.getSinais() != null) {
            empresa.getSinais().setEmpresa(empresa);
        }
        if (empresa.getRedes() != null) {
            empresa.getRedes().forEach(rede -> rede.setEmpresa(empresa));
        }
    }
}
