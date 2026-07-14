package com.sharcky.klientt.empresa.mapper;

import com.sharcky.klientt.cnpj.dto.EmpresaPayload;
import com.sharcky.klientt.cnpj.dto.SocioPayload;
import com.sharcky.klientt.empresa.model.Empresa;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

class EmpresaPayloadMapperSociosTest {

    private final EmpresaPayloadMapper mapper = new EmpresaPayloadMapperImpl();

    @Test
    void mapeiaSociosDoPayloadParaAEntidadeSemDuplicar() {
        EmpresaPayload payload = new EmpresaPayload(
                "Acme", "12345678000199", null, null, null, "SAO PAULO", null, null, null, null,
                List.of(), List.of(),
                List.of(
                        new SocioPayload("MARIA SILVA", "Administradora", "Entre 41 a 50 anos", LocalDate.of(2018, 3, 15)),
                        new SocioPayload("maria silva", "Sócia", null, null),   // duplicado por nome (case-insensitive)
                        new SocioPayload("JOAO SOUZA", "Sócio", null, null)));

        Empresa empresa = mapper.toEmpresa(payload);

        assertThat(empresa.getSocios())
                .extracting("nome", "qualificacao")
                .containsExactly(
                        tuple("MARIA SILVA", "Administradora"),
                        tuple("JOAO SOUZA", "Sócio"));
        assertThat(empresa.getSocios().get(0).getDesde()).isEqualTo(LocalDate.of(2018, 3, 15));
    }
}
