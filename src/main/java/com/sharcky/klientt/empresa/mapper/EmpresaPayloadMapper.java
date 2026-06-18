package com.sharcky.klientt.empresa.mapper;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Converte o payload da descoberta (Casa dos Dados) na entidade {@link Empresa}.
 * Os contactos são derivados na cache (telefone/email → tabela contatos).
 */
@Mapper(componentModel = "spring")
public interface EmpresaPayloadMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "atualizadoEm", ignore = true)
    @Mapping(target = "contatos", ignore = true)
    @Mapping(target = "razaoSocial", source = "cadastrais.razaoSocial")
    @Mapping(target = "nomeFantasia", source = "cadastrais.nomeFantasia")
    @Mapping(target = "situacaoCadastral", source = "cadastrais.situacaoCadastral")
    @Mapping(target = "dataAbertura", source = "cadastrais.dataAbertura")
    @Mapping(target = "capitalSocial", source = "cadastrais.capitalSocial")
    @Mapping(target = "porte", source = "cadastrais.porte")
    @Mapping(target = "naturezaJuridica", source = "cadastrais.naturezaJuridica")
    @Mapping(target = "cnaePrincipal", source = "cadastrais.cnaePrincipal")
    @Mapping(target = "optanteSimples", source = "cadastrais.optanteSimples")
    @Mapping(target = "optanteMei", source = "cadastrais.optanteMei")
    Empresa toEmpresa(EmpresaPayload payload);
}
