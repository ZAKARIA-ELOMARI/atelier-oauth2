package ma.formation.demandesapi;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class DemandeController {

    public record Demande(Long id, String owner, String objet, String statut) {
    }

    public record NouvelleDemande(String objet) {
    }

    public record StatutDemande(String statut) {
    }

    private static final List<String> STATUTS = List.of("EN_ATTENTE", "VALIDEE", "REFUSEE");

    private final AtomicLong sequence = new AtomicLong(0);

    // Stockage en memoire, trie par identifiant. Perdu au redemarrage de Spring Boot.
    private final Map<Long, Demande> demandes = new ConcurrentSkipListMap<>();

    @GetMapping("/api/public")
    public Map<String, String> publicApi() {
        return Map.of("message", "API publique accessible");
    }

    /** Les demandes de l'utilisateur connecte : le proprietaire vient du claim "sub" du JWT. */
    @GetMapping("/api/demandes")
    public List<Demande> mesDemandes(@AuthenticationPrincipal Jwt jwt) {
        return demandes.values().stream()
                .filter(d -> d.owner().equals(jwt.getSubject()))
                .toList();
    }

    @PostMapping("/api/demandes")
    public Demande creer(@AuthenticationPrincipal Jwt jwt, @RequestBody NouvelleDemande nouvelle) {

        if (nouvelle == null || nouvelle.objet() == null || nouvelle.objet().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Objet obligatoire");
        }

        Demande demande = new Demande(
                sequence.incrementAndGet(),
                jwt.getSubject(),
                nouvelle.objet().trim(),
                "EN_ATTENTE");

        demandes.put(demande.id(), demande);
        return demande;
    }

    @GetMapping("/api/admin/demandes")
    public List<Demande> toutesLesDemandes() {
        return List.copyOf(demandes.values());
    }

    /** Valider ou refuser une demande (ADMIN). Mise a jour atomique, l'ordre de la liste est conserve. */
    @PatchMapping("/api/admin/demandes/{id}")
    public Demande changerStatut(@PathVariable Long id, @RequestBody StatutDemande body) {

        if (body == null || body.statut() == null || !STATUTS.contains(body.statut())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide");
        }

        Demande modifiee = demandes.computeIfPresent(id,
                (cle, d) -> new Demande(d.id(), d.owner(), d.objet(), body.statut()));

        if (modifiee == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Demande introuvable");
        }
        return modifiee;
    }
}
