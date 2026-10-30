package ma.adria.gateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.*;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
@Configuration
public class SecurityConfig {
    // Docker : jeton Keycloak obligatoire sur toutes les routes, sauf la sante pour les healthchecks.
    @Bean @ConditionalOnProperty(name="bank.security.enabled", havingValue="true")
    SecurityWebFilterChain secured(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth.pathMatchers("/actuator/health/**", "/actuator/info").permitAll().anyExchange().authenticated())
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults()))
            .build();
    }
    // LANCER.ps1 : pas de Keycloak, la gateway reste ouverte comme avant.
    @Bean @ConditionalOnProperty(name="bank.security.enabled", havingValue="false", matchIfMissing=true)
    SecurityWebFilterChain open(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
            .authorizeExchange(auth -> auth.anyExchange().permitAll())
            .build();
    }
}
