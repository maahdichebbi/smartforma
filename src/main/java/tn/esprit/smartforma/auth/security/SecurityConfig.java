package tn.esprit.smartforma.auth.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(this::commenceUnauthorized)
                        .accessDeniedHandler(this::handleAccessDenied)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register", "/api/v1/auth/login").permitAll()

                        // Public catalogue + formation details (including chapters, sessions, seat stats)
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/formations",
                                "/api/v1/formations/*",
                                "/api/v1/formations/search",
                                "/api/v1/formations/*/chapitres",
                                "/api/v1/formations/*/chapitres/*",
                                "/api/v1/categories",
                                "/api/v1/categories/*",
                                "/api/v1/sessions",
                                "/api/v1/sessions/*",
                                "/api/v1/sessions/disponibles",
                                "/api/v1/sessions/formation/*",
                                "/api/v1/sessions/formation/*/disponibles",
                                "/api/v1/inscriptions/session/*/stats"
                        ).permitAll()

                        // Catalogue / session mutations — ADMIN
                        .requestMatchers(HttpMethod.POST, "/api/v1/categories", "/api/v1/formations",
                                "/api/v1/formations/*/chapitres", "/api/v1/sessions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/categories/*", "/api/v1/formations/*",
                                "/api/v1/formations/*/chapitres/*", "/api/v1/sessions/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/categories/*", "/api/v1/formations/*",
                                "/api/v1/formations/*/chapitres/*", "/api/v1/sessions/*").hasRole("ADMIN")

                        // Admin registration management
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions/session/*/attente").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions/session/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/inscriptions/*/confirmer").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/v1/apprenants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/apprenants").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/apprenants/*").hasRole("ADMIN")

                        // Exports (PDF & Excel reporting) — ADMIN
                        .requestMatchers("/api/v1/exports/**").hasRole("ADMIN")

                        // Learner (or admin for path-id admin tools) — identity still checked in controllers
                        .requestMatchers(HttpMethod.GET, "/api/v1/auth/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions/me").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/recommendations/me").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/apprenants/me").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/apprenants/me").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.POST, "/api/v1/inscriptions/session/*").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions/apprenant/*").hasAnyRole("ADMIN", "LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/inscriptions/*").hasAnyRole("ADMIN", "LEARNER")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/inscriptions/*/annuler").hasAnyRole("ADMIN", "LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/recommendations/**").hasRole("LEARNER")
                        .requestMatchers(HttpMethod.GET, "/api/v1/apprenants/*").hasAnyRole("ADMIN", "LEARNER")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/apprenants/*").hasAnyRole("ADMIN", "LEARNER")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:4200", "http://127.0.0.1:4200"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private void commenceUnauthorized(HttpServletRequest request, HttpServletResponse response,
                                      org.springframework.security.core.AuthenticationException exception)
            throws java.io.IOException {
        SecurityErrorWriter.write(
                response,
                HttpStatus.UNAUTHORIZED.value(),
                "Authentification requise."
        );
    }

    private void handleAccessDenied(HttpServletRequest request, HttpServletResponse response,
                                    org.springframework.security.access.AccessDeniedException exception)
            throws java.io.IOException {
        SecurityErrorWriter.write(
                response,
                HttpStatus.FORBIDDEN.value(),
                "Vous n'êtes pas autorisé à accéder à cette ressource."
        );
    }
}
