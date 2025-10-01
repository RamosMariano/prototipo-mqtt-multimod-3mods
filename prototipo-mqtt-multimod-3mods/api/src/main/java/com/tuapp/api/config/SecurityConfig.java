package com.tuapp.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    // 1. Configura el filtro de seguridad para usar Basic Auth
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // 1. Updated CSRF Configuration using Lambda DSL
            .csrf(csrf -> csrf.disable()) 
            
            // 2. Updated Authorize Requests Configuration (already correct)
            .authorizeHttpRequests(auth -> auth
                .anyRequest().authenticated()
            )
            
            // 3. Updated HTTP Basic Configuration using Lambda DSL
            .httpBasic(Customizer.withDefaults()); // Use Customizer for default Basic Auth

        return http.build();
    }
    // You will need to add one import for 'Customizer':
    // import org.springframework.security.config.Customizer;

    // 2. Define las credenciales del usuario "IoTEste"
    @Bean
    public UserDetailsService userDetailsService() {
        
        UserDetails user = User.builder()
            .username("IoTEste") 
            // **IMPORTANTE:** La contraseña debe ser codificada (encriptada)
            .password(passwordEncoder().encode("1234")) 
            .roles("USER")
            .build();
            
        // Usa un administrador de detalles de usuario en memoria para desarrollo
        return new InMemoryUserDetailsManager(user);
    }

    // 3. Define el codificador de contraseñas (necesario para Spring Security)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); 
    }
}