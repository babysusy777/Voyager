package it.unipi.Voyager.config;


import it.unipi.Voyager.model.Traveller;
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
    //Bean per l'hashing sicuro delle password
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

                        .requestMatchers("/host/**").hasAuthority("HOST")
                        .requestMatchers("/traveller/**").hasAuthority("TRAVELLER")

                        .anyRequest().authenticated()
                )
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
    @Autowired
    private TravellerRepository travellerRepository;

    @Bean
    public UserDetailsService userDetailsService() {
        return email-> {
            // Spring Security cerca l'utente nel tuo MongoDB usando il repository
            it.unipi.Voyager.model.Traveller user = travellerRepository.findByEmail(email)
                    .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));

            return org.springframework.security.core.userdetails.User.builder()
                    .password(user.getPassword()) // La password hashata nel DB
                    .authorities(user.getRole().name()) // Es: "TRAVELLLER"
                    .build();
        };
    }


}
