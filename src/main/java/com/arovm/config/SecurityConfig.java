package com.arovm.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. Disable CSRF (Required for your API calls and WebSockets to work from the frontend)
                .csrf(csrf -> csrf.disable())

                // 2. Configure Permissions
                .authorizeHttpRequests(auth -> auth
                        // Allow public access to these routes
                        .requestMatchers("/", "/register", "/login", "/css/**", "/js/**", "/images/**", "/founder").permitAll()
                        // Explicitly allow WebSocket endpoint
                        .requestMatchers("/terminal/**").permitAll()
                        // Everything else (like /ide and /api/**) requires login
                        .anyRequest().authenticated()
                )

                // 3. Login Configuration
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )

                // 4. Logout Configuration
                .logout(logout -> logout
                        .logoutSuccessUrl("/")
                        .permitAll()
                )

                // 5. Allow iframes (Important if you use the Web Preview iframe in your IDE)
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                );

        return http.build();
    }
}