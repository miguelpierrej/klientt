package com.sharcky.klientt.procon.client;

/**
 * Um registo cru obtido da fonte Procon. O domínio é normalizado pelo serviço.
 */
public record ProconRegisto(String dominio, String razaoSocial, String cnpj) {
}
