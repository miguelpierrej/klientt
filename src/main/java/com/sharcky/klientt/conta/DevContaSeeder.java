package com.sharcky.klientt.conta;

import com.sharcky.klientt.conta.model.Utilizador;
import com.sharcky.klientt.conta.repository.PlanoRepository;
import com.sharcky.klientt.conta.repository.UtilizadorRepository;
import com.sharcky.klientt.perfil.PerfilCliente;
import com.sharcky.klientt.perfil.PerfilClienteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cria um utilizador de DEV no arranque, se não existir nenhum.
 * Temporário: facilita o login durante o desenvolvimento.
 */
@Component
public class DevContaSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevContaSeeder.class);
    private static final String DEV_EMAIL = "dev@klientt.com";

    private final UtilizadorRepository utilizadorRepository;
    private final PlanoRepository planoRepository;
    private final PasswordEncoder passwordEncoder;
    private final PerfilClienteRepository perfilRepository;

    public DevContaSeeder(UtilizadorRepository utilizadorRepository, PlanoRepository planoRepository,
                          PasswordEncoder passwordEncoder, PerfilClienteRepository perfilRepository) {
        this.utilizadorRepository = utilizadorRepository;
        this.planoRepository = planoRepository;
        this.passwordEncoder = passwordEncoder;
        this.perfilRepository = perfilRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (utilizadorRepository.count() > 0) {
            return;
        }
        Utilizador u = new Utilizador();
        u.setNome("Dev");
        u.setEmail(DEV_EMAIL);
        u.setPasswordHash(passwordEncoder.encode("dev12345"));
        u.setEmailVerificado(true);   // dev entra sem passar pela confirmação de email
        // Créditos generosos em dev para poder testar o "carregar mais" sem pagar.
        u.setCreditosLeads(9000);
        planoRepository.findByNome("Agency").or(() -> planoRepository.findByNome("Teste")).ifPresent(u::setPlano);
        utilizadorRepository.save(u);

        // Perfil concluído → dev vai direto para /app (não passa pelo onboarding).
        PerfilCliente perfil = new PerfilCliente();
        perfil.setUtilizadorId(u.getId());
        perfil.setConcluido(true);
        perfilRepository.save(perfil);

        log.info("[DEV] utilizador criado: {} / dev12345", DEV_EMAIL);
    }
}
