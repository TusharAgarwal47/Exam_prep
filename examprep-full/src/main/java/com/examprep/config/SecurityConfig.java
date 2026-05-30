package com.examprep.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;

@Configuration
public class SecurityConfig {

    /**
     * Expose BCryptPasswordEncoder as a Spring bean so it can be
     * injected into AuthService and DataSeeder via @RequiredArgsConstructor.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Permit all API endpoints (auth handled manually in AuthService).
     * Disables CSRF since this is a stateless REST + Swing client setup.
     * Disables X-Frame-Options so the H2 console (which uses iframes) works.
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // Allow H2 console to render inside its own frames
            .headers(headers -> headers
                .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/h2-console/**").permitAll()
                .anyRequest().permitAll()
            );
        return http.build();
    }
}
