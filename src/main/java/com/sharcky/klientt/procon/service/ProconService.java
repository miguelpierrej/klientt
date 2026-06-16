package com.sharcky.klientt.procon.service;

/**
 * Responde se o site de uma empresa consta na lista Procon-SP "Evite Sites".
 * Interface funcional: fácil de substituir por um stub nos testes de scoring.
 */
@FunctionalInterface
public interface ProconService {

    /** true se o domínio do website constar na lista Procon sincronizada. */
    boolean constaNoProcon(String website);
}
