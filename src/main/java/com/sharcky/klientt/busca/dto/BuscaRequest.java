package com.sharcky.klientt.busca.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Parâmetros de uma busca submetidos pelo formulário.
 * Validado com Jakarta Validation no controlador.
 */
public record BuscaRequest(

        @NotNull(message = "Selecione o tipo de busca.")
        TipoBusca tipo,

        @NotBlank(message = "Indique o que procura.")
        @Size(max = 255, message = "O termo é demasiado longo.")
        String termo,

        @Size(max = 120, message = "A região é demasiado longa.")
        String regiao
) {
    public boolean temRegiao() {
        return regiao != null && !regiao.isBlank();
    }
}
