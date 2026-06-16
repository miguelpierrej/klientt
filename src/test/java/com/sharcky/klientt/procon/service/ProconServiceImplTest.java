package com.sharcky.klientt.procon.service;

import com.sharcky.klientt.procon.config.ProconProperties;
import com.sharcky.klientt.procon.model.ProconEviteSite;
import com.sharcky.klientt.procon.repository.ProconEviteSiteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProconServiceImplTest {

    @Mock
    ProconEviteSiteRepository repository;

    ProconServiceImpl service;

    @BeforeEach
    void setUp() {
        // Sem fonte → não sincroniza (modo desligado).
        service = new ProconServiceImpl(repository, new ProconProperties(), Optional.empty());
    }

    @Test
    void normalizaDominioAoComparar() {
        lenient().when(repository.findAll()).thenReturn(List.of(dominio("loja-ruim.com.br")));
        service.carregarAoArrancar();

        // Variações de esquema/www/caminho devem todas bater no mesmo domínio.
        assertThat(service.constaNoProcon("https://www.loja-ruim.com.br/contato")).isTrue();
        assertThat(service.constaNoProcon("http://loja-ruim.com.br")).isTrue();
        assertThat(service.constaNoProcon("loja-ruim.com.br")).isTrue();
        assertThat(service.constaNoProcon("https://outra-loja.com.br")).isFalse();
        assertThat(service.constaNoProcon(null)).isFalse();
    }

    @Test
    void semFonteNaoVaiABuscarNada() {
        service.sincronizar();
        verify(repository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void dominioDeRemoveEsquemaEWww() {
        assertThat(ProconServiceImpl.dominioDe("https://www.exemplo.com/x")).isEqualTo("exemplo.com");
        assertThat(ProconServiceImpl.dominioDe("exemplo.com")).isEqualTo("exemplo.com");
        assertThat(ProconServiceImpl.dominioDe("  ")).isNull();
        assertThat(ProconServiceImpl.dominioDe(null)).isNull();
    }

    private ProconEviteSite dominio(String d) {
        ProconEviteSite e = new ProconEviteSite();
        e.setDominio(d);
        return e;
    }
}
