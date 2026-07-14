package com.sharcky.klientt.conta.seguranca;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        // Páginas públicas + callback do scraper (autenticado pelo token X-Klientt-Token).
                        .requestMatchers("/", "/login", "/registo", "/css/**", "/favicon.svg",
                                "/api/scraper/**", "/api/stripe/**").permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/app", true)
                        .permitAll())
                .logout(logout -> logout
                        .logoutSuccessUrl("/login?logout")
                        .permitAll())
                // Sessão expirada: num pedido HTMX manda redirecionar a página inteira p/ o login
                // (senão o htmx injeta o HTML do login dentro do fragmento — o "ecrã bugado").
                .exceptionHandling(ex -> ex.authenticationEntryPoint(entryPointHtmxAware()))
                // Callbacks externos (scraper, webhook Stripe) não têm sessão → isentos de CSRF.
                .csrf(csrf -> csrf.ignoringRequestMatchers("/api/scraper/**", "/api/stripe/**"));
        return http.build();
    }

    /**
     * Redireciona para o login de forma amigável ao HTMX: se o pedido é HTMX ({@code HX-Request}),
     * devolve {@code HX-Redirect} (o htmx recarrega a página inteira); senão, o redirect 302 normal.
     */
    private static AuthenticationEntryPoint entryPointHtmxAware() {
        LoginUrlAuthenticationEntryPoint padrao = new LoginUrlAuthenticationEntryPoint("/login");
        return (request, response, authException) -> {
            if ("true".equalsIgnoreCase(request.getHeader("HX-Request"))) {
                response.setHeader("HX-Redirect", "/login?expirou");
                response.setStatus(HttpServletResponse.SC_OK);
            } else {
                padrao.commence(request, response, authException);
            }
        };
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
