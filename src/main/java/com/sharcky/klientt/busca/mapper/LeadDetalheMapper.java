package com.sharcky.klientt.busca.mapper;

import com.sharcky.klientt.busca.dto.ContatoView;
import com.sharcky.klientt.busca.dto.LeadDetalhe;
import com.sharcky.klientt.busca.dto.RedeView;
import com.sharcky.klientt.busca.dto.SocioView;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import com.sharcky.klientt.empresa.model.EmpresaRede;
import com.sharcky.klientt.empresa.model.EmpresaSocio;
import org.mapstruct.Mapper;

/**
 * Converte {@link Empresa} na vista completa {@link LeadDetalhe} (contactos + cadastrais + redes).
 * {@code contactavel} vem de {@code Empresa.isContactavel()}.
 */
@Mapper(componentModel = "spring")
public interface LeadDetalheMapper {

    LeadDetalhe toDetalhe(Empresa empresa);

    ContatoView toContatoView(Contato contato);

    RedeView toRedeView(EmpresaRede rede);

    SocioView toSocioView(EmpresaSocio socio);
}
