package com.sharcky.klientt.cnae;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class SinonimoCnaeTest {

    @Mock CnaeCatalogoRepository catalogo;
    SinonimoCnae sinonimos;

    @BeforeEach
    void setUp() {
        // Só estes códigos são "válidos" no catálogo → sinónimos com outros códigos são ignorados.
        lenient().when(catalogo.findAll()).thenReturn(List.of(
                cat("8630504"), cat("5611201"), cat("5611205"), cat("9602501"),
                cat("4711302"), cat("4712100")));
        sinonimos = new SinonimoCnae(catalogo);
    }

    private CnaeCatalogo cat(String cod) {
        CnaeCatalogo c = new CnaeCatalogo();
        c.setCodigo(cod);
        c.setDescricao("desc");
        return c;
    }

    @Test
    void resolveSinonimoExato() {
        assertThat(sinonimos.codigosPara("dentista")).containsExactly("8630504");
    }

    @Test
    void resolvePluralEComFrase() {
        assertThat(sinonimos.codigosPara("restaurantes")).containsExactly("5611201");
        assertThat(sinonimos.codigosPara("procuro dentista em bauru")).contains("8630504");
    }

    @Test
    void naoCasaSubstringDeOutraPalavra() {
        // "barbearia" contém "bar" como substring, mas NÃO como palavra → só cabeleireiro.
        List<String> r = sinonimos.codigosPara("barbearia");
        assertThat(r).containsExactly("9602501");
        assertThat(r).doesNotContain("5611205");   // "bar"
    }

    @Test
    void autocompleteCasaParcial() {
        assertThat(sinonimos.codigosPorTexto("denti")).contains("8630504");
        assertThat(sinonimos.codigosPorTexto("resta")).contains("5611201");
    }

    @Test
    void codigoForaDoCatalogoEhIgnorado() {
        // "farmacia" aponta para 4771701, que não está no catálogo mockado → não carrega.
        assertThat(sinonimos.codigosPara("farmacia")).isEmpty();
    }

    @Test
    void termoCurtoOuVazioNaoResolve() {
        assertThat(sinonimos.codigosPara("")).isEmpty();
        assertThat(sinonimos.codigosPorTexto("d")).isEmpty();
    }
}
