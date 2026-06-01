package com.sharcky.klientt.conta.seguranca;

import com.sharcky.klientt.conta.model.Utilizador;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Principal autenticado — adapta {@link Utilizador} ao contrato do Spring Security
 * e expõe o id (útil para associar jobs ao utilizador).
 */
public class KlienttUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String passwordHash;
    private final String nome;

    public KlienttUserDetails(Utilizador utilizador) {
        this.id = utilizador.getId();
        this.email = utilizador.getEmail();
        this.passwordHash = utilizador.getPasswordHash();
        this.nome = utilizador.getNome();
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
