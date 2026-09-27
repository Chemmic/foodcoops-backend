package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.FinaleBestellung;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.GebindeAenderung;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.NeuePosition;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.Position;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungService;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungService.FinaleBestellungException;

/**
 * ============================================================================
 * Finale Bestellung festlegen
 * ============================================================================
 *
 * Berechtigung (SecurityConfiguration):
 *
 *   /organisation/**   -> Organisator oder Admin
 *
 * Es werden nur die Gebinde der Bestellübersicht geändert – die
 * Bestellungen der Mitglieder bleiben unverändert.
 *
 * ============================================================================
 */
@RestController
public class FinaleBestellungController {

    private final FinaleBestellungService service;

    public FinaleBestellungController(FinaleBestellungService service) {
        this.service = service;
    }


    @GetMapping("/organisation/bestellung")
    public FinaleBestellung bestellung(
            @RequestParam(defaultValue = "12") int verlauf
    ) {
        return service.laden(Math.max(0, Math.min(verlauf, 52)));
    }


    @PutMapping("/organisation/bestellung/produkte/{produktId}")
    public Position gebindeSetzen(
            @PathVariable String produktId,
            @RequestBody GebindeAenderung body
    ) {
        return service.gebindeSetzen(produktId, body == null ? null : body.gebinde());
    }


    @PostMapping("/organisation/bestellung/produkte")
    public Position hinzufuegen(
            @RequestBody NeuePosition body
    ) {
        return service.hinzufuegen(
                body == null ? null : body.produktId(),
                body == null ? null : body.gebinde()
        );
    }


    @DeleteMapping("/organisation/bestellung/produkte/{produktId}")
    public ResponseEntity<Void> entfernen(
            @PathVariable String produktId
    ) {
        service.entfernen(produktId);
        return ResponseEntity.noContent().build();
    }


    @ExceptionHandler(FinaleBestellungException.class)
    public ResponseEntity<Map<String, String>> fehler(FinaleBestellungException exception) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(Map.of("message", exception.getMessage()));
    }
}
