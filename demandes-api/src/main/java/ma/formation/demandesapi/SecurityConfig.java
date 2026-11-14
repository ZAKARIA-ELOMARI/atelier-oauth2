package ma.formation.demandesapi;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        return http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // pre-requete CORS du navigateur
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        // API publique
                        .requestMatchers(HttpMethod.GET, "/api/public").permitAll()
                        // page d'erreur interne de Spring : sans cela, un 400 ou un 404
                        // peut etre masque par un 401
                        .requestMatchers("/error").permitAll()
                        // reserve au role ADMIN
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        // reserve aux utilisateurs ayant le role USER ou ADMIN
                        .requestMatchers("/api/demandes/**", "/api/profil").hasAnyRole("USER", "ADMIN")
                        .anyRequest().authenticated())
                // Spring Boot est un Resource Server : il valide les Access Tokens JWT
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtConverter())))
                .build();
    }

    /**
     * Keycloak place les roles du realm dans le claim "realm_access.roles".
     * On les convertit en autorites Spring : USER -> ROLE_USER, ADMIN -> ROLE_ADMIN.
     */
    @Bean
    JwtAuthenticationConverter jwtConverter() {

        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> result = new ArrayList<>(scopes.convert(jwt));

            Map<String, Object> access = jwt.getClaimAsMap("realm_access");
            if (access != null && access.get("roles") instanceof List<?> roles) {
                for (Object role : roles) {
                    result.add(new SimpleGrantedAuthority("ROLE_" + role));
                }
            }
            return result;
        });

        return converter;
    }

    /** CORS : autorise uniquement le frontend React. */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
