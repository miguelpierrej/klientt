package com.sharcky.klientt.conta.seguranca;

import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class KlienttUserDetailsService implements UserDetailsService {

    private final UtilizadorRepository utilizadorRepository;

    public KlienttUserDetailsService(UtilizadorRepository utilizadorRepository) {
        this.utilizadorRepository = utilizadorRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return utilizadorRepository.findByEmail(email)
                .map(KlienttUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado: " + email));
    }
}
