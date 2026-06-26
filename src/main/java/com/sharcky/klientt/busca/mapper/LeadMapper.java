package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.LeadResponse;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.Mapper;

/**
 * Converte {@link Empresa} no DTO de resposta {@link LeadResponse} (cópia de campos).
 * {@code contactavel} vem de {@code Empresa.isContactavel()}.
 */
@Mapper(componentModel = "spring")
public interface LeadMapper {

    LeadResponse toResponse(Empresa empresa);
}
