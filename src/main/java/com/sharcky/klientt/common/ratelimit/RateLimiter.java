package com.sharcky.klientt.common.ratelimit;

import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiter em memória (token bucket por chave). Adequado a uma instância única (Railway).
 * NB: o estado é por processo — se a app escalar horizontalmente, migrar para um store partilhado
 * (Redis/Bucket4j) para que o limite seja global.
 */
@Component
public class RateLimiter {

    /** Teto de chaves distintas em memória (evita que o próprio limiter vire vetor de OOM). */
    private static final int MAX_BUCKETS = 50_000;

    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    /**
     * Consome 1 token da chave. Devolve {@code true} se dentro do limite, {@code false} se excedeu.
     * A capacidade/janela ficam associadas à chave na primeira chamada.
     */
    public boolean tryAcquire(String chave, int capacidade, Duration janela) {
        if (buckets.size() >= MAX_BUCKETS) {
            removerOciosos();
        }
        Bucket bucket = buckets.computeIfAbsent(chave, k -> new Bucket(capacidade, janela.toNanos()));
        return bucket.tryConsume();
    }

    /** Remove baldes cheios (chaves ociosas) para libertar memória. */
    private void removerOciosos() {
        buckets.values().removeIf(Bucket::cheio);
    }

    /** Token bucket com recarga contínua, thread-safe. */
    private static final class Bucket {
        private final double capacidade;
        private final double tokensPorNano;
        private double tokens;
        private long ultimoNano;

        Bucket(int capacidade, long janelaNanos) {
            this.capacidade = capacidade;
            this.tokensPorNano = (double) capacidade / janelaNanos;
            this.tokens = capacidade;
            this.ultimoNano = System.nanoTime();
        }

        synchronized boolean tryConsume() {
            recarregar();
            if (tokens >= 1d) {
                tokens -= 1d;
                return true;
            }
            return false;
        }

        synchronized boolean cheio() {
            recarregar();
            return tokens >= capacidade;
        }

        private void recarregar() {
            long agora = System.nanoTime();
            tokens = Math.min(capacidade, tokens + (agora - ultimoNano) * tokensPorNano);
            ultimoNano = agora;
        }
    }
}
