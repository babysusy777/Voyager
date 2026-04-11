package it.unipi.Voyager.config;

import it.unipi.Voyager.repository.HostRepository;
import it.unipi.Voyager.repository.TravellerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Autowired
    private TravellerRepository travellerRepository;

    @Autowired
    private HostRepository hostRepository;

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/",
                                "/error",
                                "/api-docs/**",
                                "/api-docs",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api/auth/**"
                        ).permitAll()

                        // Endpoint HOST
                        .requestMatchers("/api/host/**").hasAuthority("HOST")
                        .requestMatchers("/api/hotels/host/**").hasAuthority("HOST")
                        .requestMatchers("/api/cities/host/**").hasAuthority("HOST")

                        // Endpoint TRAVELLER
                        .requestMatchers("/api/traveller/**").hasAuthority("TRAVELLER")
                        .requestMatchers("/api/trips/traveller/**").hasAuthority("TRAVELLER")
                        .requestMatchers("/api/cities/traveller/**").hasAuthority("TRAVELLER")

                        // Endpoint pubblici/condivisi
                        .requestMatchers("/api/hotels/search").authenticated()
                        .requestMatchers("/api/cities/top-attractions").authenticated()

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return email -> {
            if (email.endsWith("@voyager.com")) {
                // Se l'email è del dominio voyager.com, cerchiamo solo tra gli HOST
                var host = hostRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("Host non trovato con email: " + email));
                return org.springframework.security.core.userdetails.User.builder()
                        .username(host.getEmail())
                        .password(host.getPassword())
                        .authorities(host.getRole().name()) // Restituisce "HOST"
                        .build();
            } else {
                // Altrimenti, cerchiamo tra i TRAVELLER
                var traveller = travellerRepository.findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("Viaggiatore non trovato con email: " + email));

                return org.springframework.security.core.userdetails.User.builder()
                        .username(traveller.getEmail())
                        .password(traveller.getPassword())
                        .authorities(traveller.getRole().name()) // Restituisce "TRAVELLER"
                        .build();
            }

        };
    }
}
