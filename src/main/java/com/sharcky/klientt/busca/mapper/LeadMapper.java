package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converte {@link Empresa} no DTO de resposta {@link LeadResponse} (cópia de campos).
 * {@code contactavel} vem de {@code Empresa.isContactavel()}. O {@code fit} é preenchido depois
 * (por request, via {@link LeadResponse#comFit}), não pelo mapper.
 */
@Mapper(componentModel = "spring")
public interface LeadMapper {

    @Mapping(target = "fit", ignore = true)
    LeadResponse toResponse(Empresa empresa);
}
