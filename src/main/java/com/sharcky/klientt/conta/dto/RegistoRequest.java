package com.sharcky.klientt.conta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de registo.
 */
public record RegistoRequest(

        @NotBlank(message = "Indique o seu nome.")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "Indique o seu email.")
        @Email(message = "Email inválido.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Defina uma password.")
        @Size(min = 8, max = 100, message = "A password deve ter pelo menos 8 caracteres.")
        String password
) {
}
