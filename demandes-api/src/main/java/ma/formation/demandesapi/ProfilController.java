package ma.formation.demandesapi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ProfilController {

    public record Profil(String id, String username, List<String> roles) {
    }

    /** Toutes les donnees viennent du JWT de l'utilisateur authentifie. */
    @GetMapping("/api/profil")
    public Profil profil(@AuthenticationPrincipal Jwt jwt) {

        List<String> roles = new ArrayList<>();
        Map<String, Object> access = jwt.getClaimAsMap("realm_access");
        if (access != null && access.get("roles") instanceof List<?> liste) {
            for (Object role : liste) {
                roles.add(String.valueOf(role));
            }
        }

        return new Profil(jwt.getSubject(), jwt.getClaimAsString("preferred_username"), roles);
    }
}
