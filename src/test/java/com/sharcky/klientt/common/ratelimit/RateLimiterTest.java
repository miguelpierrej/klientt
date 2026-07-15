package com.sharcky.klientt.common.ratelimit;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RateLimiterTest {

    @Test
    void permiteAteACapacidadeEDepoisBloqueia() {
        RateLimiter limiter = new RateLimiter();
        Duration janela = Duration.ofMinutes(10);   // recarga desprezível durante o teste

        for (int i = 0; i < 3; i++) {
            assertThat(limiter.tryAcquire("k", 3, janela)).as("pedido %d", i).isTrue();
        }
        assertThat(limiter.tryAcquire("k", 3, janela)).isFalse();   // 4º excede
    }

    @Test
    void chavesDiferentesSaoIndependentes() {
        RateLimiter limiter = new RateLimiter();
        Duration janela = Duration.ofMinutes(10);

        assertThat(limiter.tryAcquire("a", 1, janela)).isTrue();
        assertThat(limiter.tryAcquire("a", 1, janela)).isFalse();   // 'a' esgotado
        assertThat(limiter.tryAcquire("b", 1, janela)).isTrue();    // 'b' intacto
    }

    @Test
    void recarregaComOTempo() {
        // Relógio controlado → determinístico (sem depender de wall-clock, que é flaky sob carga).
        long[] agoraNanos = {0L};
        RateLimiter limiter = new RateLimiter(() -> agoraNanos[0]);
        Duration janela = Duration.ofMillis(200);   // 2 tokens / 200ms

        assertThat(limiter.tryAcquire("k", 2, janela)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, janela)).isTrue();
        assertThat(limiter.tryAcquire("k", 2, janela)).isFalse();   // esgotado, tempo parado

        agoraNanos[0] += Duration.ofMillis(250).toNanos();   // avança o relógio > janela → recarrega
        assertThat(limiter.tryAcquire("k", 2, janela)).isTrue();
    }
}
