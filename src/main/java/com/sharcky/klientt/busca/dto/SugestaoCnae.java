package com.sharcky.klientt.busca.dto;

/**
 * Sugestão de atividade (CNAE) para o autocomplete. {@code codigo} = 7 dígitos (vai no campo oculto
 * que confirma o CNAE); {@code codigoFormatado} = "0000-0/00" para exibição.
 */
public record SugestaoCnae(String codigo, String codigoFormatado, String descricao) {

    public static SugestaoCnae de(String codigo, String descricao) {
        return new SugestaoCnae(codigo, formatar(codigo), descricao);
    }

    private static String formatar(String c) {
        if (c == null || c.length() != 7) {
            return c;
        }
        return c.substring(0, 4) + "-" + c.substring(4, 5) + "/" + c.substring(5, 7);
    }
}
