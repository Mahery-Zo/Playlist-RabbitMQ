package mg.itu.naina.api.config;

import mg.itu.naina.api.auth.JwtFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Auth endpoints ouverts
                .requestMatchers("/api/auth/**").permitAll()
                // Streaming audio ouvert (pour <audio>)
                .requestMatchers(HttpMethod.GET, "/api/songs/*/stream").permitAll()
                // Lecture des chansons ouverte (P3 POST aussi)
                .requestMatchers(HttpMethod.GET, "/api/songs", "/api/songs/**").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/songs").permitAll()
                // Admin endpoints (authentifié)
                .requestMatchers("/api/admin/**").authenticated()
                // Tout le reste nécessite un JWT
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
