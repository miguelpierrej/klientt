package com.sharcky.klientt.procon.client;

import java.util.List;

/**
 * Fonte da lista Procon-SP "Evite Sites". Existe um bean apenas quando
 * klientt.procon.enabled=true (ver {@link HttpProconFonte}); caso contrário o
 * serviço opera sem sincronizar.
 */
public interface ProconEviteSitesFonte {

    List<ProconRegisto> obter();
}
