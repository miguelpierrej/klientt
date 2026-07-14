package com.sharcky.klientt.conta.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de nova password (recuperação).
 */
public record RedefinirSenhaRequest(

        @NotBlank
        String token,

        @NotBlank(message = "Defina uma senha.")
        @Size(min = 8, max = 100, message = "A senha deve ter pelo menos 8 caracteres.")
        String password
) {
}
