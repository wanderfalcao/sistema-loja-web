package br.com.infnet.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Configuração mínima de segurança: headers HTTP e restrição do actuator.
 * CSRF desabilitado — aplicação sem autenticação de usuário.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                .requestMatchers("/actuator/**").denyAll()
                .anyRequest().permitAll()
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
                .contentTypeOptions(opt -> {})
                .referrerPolicy(ref ->
                    ref.policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
            )
            .csrf(csrf -> csrf.disable());

        return http.build();
    }
}
