package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Historie;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Mitglied;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Statistik;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieService;

/**
 * ============================================================================
 * Bestellhistorie & Statistik
 * ============================================================================
 *
 * Berechtigungen (SecurityConfiguration):
 *
 *   /me/**      -> angemeldet; die Person kommt aus dem Token, nicht aus
 *                  der URL – man sieht nur die eigene Historie.
 *   /admin/**   -> Admin
 *
 * ============================================================================
 */
@RestController
public class HistorieController {

    private final HistorieService service;

    public HistorieController(HistorieService service) {
        this.service = service;
    }


    @GetMapping("/me/historie")
    public Historie meineHistorie(@AuthenticationPrincipal Jwt jwt) {
        String personId = jwt.getClaimAsString("preferred_username");

        if (personId == null || personId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Im Token fehlt der Benutzername."
            );
        }

        return service.historie(personId);
    }


    @GetMapping("/admin/mitglieder")
    public List<Mitglied> mitglieder() {
        return service.mitglieder();
    }


    @GetMapping("/admin/mitglieder/{personId}/historie")
    public Historie historieVon(@PathVariable String personId) {
        return service.historie(personId);
    }


    @GetMapping("/admin/statistik")
    public Statistik statistik(
            @RequestParam(defaultValue = "12") int runden
    ) {
        return service.statistik(Math.min(runden, 104));
    }
}
