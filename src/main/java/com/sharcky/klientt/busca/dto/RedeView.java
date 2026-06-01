package com.sharcky.klientt.busca.dto;

/** Rede social de uma empresa, para apresentação. */
public record RedeView(
        String rede,
        String url,
        Integer seguidores
) {
}
