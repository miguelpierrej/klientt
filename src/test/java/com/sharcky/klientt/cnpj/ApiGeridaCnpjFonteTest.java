package com.sharcky.klientt.cnpj;

import com.sharcky.klientt.cnpj.config.ClienteCnpjProperties;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ApiGeridaCnpjFonteTest {

    @Test
    void desligadaDevolveVazioSemChamarApi() {
        ClienteCnpjProperties props = new ClienteCnpjProperties();   // enabled=false por default
        FonteCnpj fonte = new ApiGeridaCnpjFonte(props);

        assertThat(fonte.buscarPorCnae("9602-5/01", "São Paulo", 50)).isEmpty();
    }
}
