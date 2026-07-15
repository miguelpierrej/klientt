package com.sharcky.klientt.perfil;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Form-backing bean do onboarding/perfil (ICP). É uma classe (não record) de propósito: assim os
 * checkboxes desmarcados (ausentes no POST) caem no default {@code false} em vez de falhar o binding.
 * {@code nichosAlvo}/{@code regioesAlvo} chegam como texto separado por vírgula (dos "chips").
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerfilForm {

    private String oferta;
    private String nichosAlvo;
    private String regioesAlvo;
    private List<String> portes = new ArrayList<>();
    private boolean querSemSite;
    private boolean querSimplesMei;
    private boolean querComContato;
}
