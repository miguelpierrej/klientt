package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.ContatoView;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.Mapper;

/**
 * Converte {@link Empresa} na vista completa {@link LeadDetalhe} (contactos + cadastrais).
 * {@code contactavel} vem de {@code Empresa.isContactavel()}.
 */
@Mapper(componentModel = "spring")
public interface LeadDetalheMapper {

    LeadDetalhe toDetalhe(Empresa empresa);

    ContatoView toContatoView(Contato contato);
}
