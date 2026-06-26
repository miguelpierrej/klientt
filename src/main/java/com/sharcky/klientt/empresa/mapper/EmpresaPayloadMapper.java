package com.sharcky.klientt.empresa.mapper;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.empresa.model.Contato;
import com.sharcky.klientt.empresa.model.Empresa;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

/**
 * Converte o payload da descoberta (Casa dos Dados) na entidade {@link Empresa}, incluindo
 * <b>todos</b> os contactos (telefones/emails) na tabela {@code contatos}.
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

    /** Acrescenta todos os telefones/emails como contactos (dedup por tipo+valor). */
    @AfterMapping
    default void preencherContatos(@MappingTarget Empresa empresa, EmpresaPayload payload) {
        adicionarContatos(empresa, "telefone", payload.telefones());
        adicionarContatos(empresa, "email", payload.emails());
    }

    private static void adicionarContatos(Empresa empresa, String tipo, List<String> valores) {
        if (valores == null) {
            return;
        }
        for (String valor : valores) {
            if (valor == null || valor.isBlank() || jaTem(empresa, tipo, valor)) {
                continue;
            }
            Contato c = new Contato();
            c.setTipo(tipo);
            c.setValor(valor.trim());
            empresa.adicionarContato(c);
        }
    }

    private static boolean jaTem(Empresa empresa, String tipo, String valor) {
        return empresa.getContatos().stream().anyMatch(c ->
                tipo.equalsIgnoreCase(c.getTipo()) && valor.trim().equalsIgnoreCase(c.getValor()));
    }
}
