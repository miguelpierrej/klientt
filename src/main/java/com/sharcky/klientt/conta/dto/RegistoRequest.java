package com.sharcky.klientt.conta.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados do formulário de registo.
 */
public record RegistoRequest(

        @NotBlank(message = "Informe seu nome.")
        @Size(max = 150)
        String nome,

        @NotBlank(message = "Informe seu e-mail.")
        @Email(message = "E-mail inválido.")
        @Size(max = 255)
        String email,

        @NotBlank(message = "Defina uma senha.")
        @Size(min = 8, max = 100, message = "A senha deve ter pelo menos 8 caracteres.")
        String password
) {
}
