package de.dhbw.foodcoop.warehouse.plugins.rest;

import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;
import de.dhbw.foodcoop.warehouse.application.lager.LagerChargenService;
import de.dhbw.foodcoop.warehouse.adapters.representations.LagerChargeRepresentation;
import java.util.Map;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.RequestParam;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.ProduktRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestandToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestandMapper;
import de.dhbw.foodcoop.warehouse.application.lager.ProduktService;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;
import de.dhbw.foodcoop.warehouse.domain.exceptions.ProduktInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.ProduktNotFoundException;

@RestController
public class ProduktController {

    private final ProduktService service;
    private final RepresentationToBestandMapper toProdukt;
    private final BestandToRepresentationMapper toPresentation;
    private final LagerChargenService chargen;

    public ProduktController(
            ProduktService service,
            RepresentationToBestandMapper toProdukt,
            BestandToRepresentationMapper toPresentation,
            LagerChargenService chargen) {

        this.service = service;
        this.toProdukt = toProdukt;
        this.toPresentation = toPresentation;
        this.chargen = chargen;
    }


    /** Neue Lieferung einlagern: Menge und Preis pro Einheit (optional). */
    public record Einlagerung(Double menge, Double preis) {
    }

    @GetMapping("/produkte/{id}")
    public ProduktRepresentation one(@PathVariable String id) {
        Produkt produkt = service.findById(id)
                .orElseThrow(() -> new ProduktNotFoundException(id));

        return mitChargen(produkt);
    }

    @GetMapping("/produkte")
    public List<ProduktRepresentation> all() {
        List<Produkt> produkte = service.all();
        Map<String, List<LagerCharge>> jeProdukt = chargen.chargen(produkte);

        return produkte.stream()
                .map(produkt -> {
                    ProduktRepresentation r = (ProduktRepresentation) toPresentation.apply(produkt);
                    r.setChargen(darstellen(jeProdukt.getOrDefault(produkt.getId(), List.of())));
                    return r;
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/produkte")
    public ResponseEntity<ProduktRepresentation> newProdukt(
            @RequestBody ProduktRepresentation newProdukt) {

        String id = newProdukt.getId() == null
                || newProdukt.getId().isBlank()
                || newProdukt.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newProdukt.getId();

        newProdukt.setId(id);

        Produkt saved = service.save(
                (Produkt) toProdukt.apply(newProdukt));

        // Anfangsbestand = erste Lieferung zum angegebenen Preis
        chargen.bestandGeaendert(saved.getId(), 0, saved.getPreis(), ist(saved), saved.getPreis(), false);

        ProduktRepresentation response = mitChargen(saved);

        return ResponseEntity
                .created(URI.create("/produkte/" + response.getId()))
                .body(response);
    }

    /** Reihenfolge der Lagerprodukte setzen. Body: IDs von oben nach unten. */
    @PutMapping("/produkte/reihenfolge")
    public List<ProduktRepresentation> reihenfolge(
            @RequestBody List<String> ids) {

        List<Produkt> produkte = service.reihenfolgeSetzen(ids);
        Map<String, List<LagerCharge>> jeProdukt = chargen.chargen(produkte);

        return produkte.stream()
                .map(produkt -> {
                    ProduktRepresentation r = (ProduktRepresentation) toPresentation.apply(produkt);
                    r.setChargen(darstellen(jeProdukt.getOrDefault(produkt.getId(), List.of())));
                    return r;
                })
                .collect(Collectors.toList());
    }


    /**
     * Produkt ändern. Mehr Ist-Bestand = neue Lieferung zum (neuen) Preis;
     * die vorhandene Ware behält ihren Preis, außer preisFuerBestand=true.
     */
    @PutMapping("/produkte/{id}")
    public ResponseEntity<ProduktRepresentation> update(
            @RequestBody ProduktRepresentation changedProdukt,
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean preisFuerBestand) {

        Produkt oldProdukt = service.findById(id)
                .orElseThrow(() -> new ProduktNotFoundException(id));

        double istVorher = ist(oldProdukt);
        BigDecimal preisVorher = oldProdukt.getPreis();

        Produkt updatedProdukt =
                (Produkt) toProdukt.update(
                        oldProdukt,
                        changedProdukt);

        Produkt saved = service.save(updatedProdukt);

        chargen.bestandGeaendert(id, istVorher, preisVorher, ist(saved), saved.getPreis(), preisFuerBestand);

        return ResponseEntity.ok(mitChargen(saved));
    }


    /**
     * Neue Lieferung einlagern. Der vorhandene Bestand behält seinen Preis
     * und wird zuerst verkauft.
     */
    @PostMapping("/produkte/{id}/einlagern")
    public ResponseEntity<?> einlagern(
            @PathVariable String id,
            @RequestBody Einlagerung einlagerung) {

        Produkt produkt = service.findById(id)
                .orElseThrow(() -> new ProduktNotFoundException(id));

        if (einlagerung == null
                || einlagerung.menge() == null
                || !(einlagerung.menge() > 0)
                || (einlagerung.preis() != null && einlagerung.preis() < 0)) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Bitte eine Menge größer 0 und einen gültigen Preis angeben."));
        }

        double istVorher = ist(produkt);
        Double soll = produkt.getLagerbestand().getSollLagerbestand();

        // Ist darf den Soll-Bestand nicht übersteigen (Regel des Lagerbestands)
        if (soll != null && istVorher + einlagerung.menge() > soll + 1.E-6) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "Das ist mehr als der Soll-Bestand (" + soll
                            + "). Bitte zuerst den Soll-Bestand erhöhen."));
        }
        BigDecimal preisVorher = produkt.getPreis();
        BigDecimal preisNeu =
                einlagerung.preis() == null
                        ? preisVorher
                        : BigDecimal.valueOf(einlagerung.preis());

        produkt.getLagerbestand().setIstLagerbestand(istVorher + einlagerung.menge());
        produkt.setPreis(preisNeu);

        Produkt saved = service.save(produkt);

        chargen.bestandGeaendert(id, istVorher, preisVorher, ist(saved), preisNeu, false);

        return ResponseEntity.ok(mitChargen(saved));
    }

    @DeleteMapping("/produkte/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws ProduktInUseException {

        service.deleteById(id);
        chargen.loeschen(id);

        return ResponseEntity.noContent().build();
    }


    private ProduktRepresentation mitChargen(Produkt produkt) {
        ProduktRepresentation r = (ProduktRepresentation) toPresentation.apply(produkt);
        r.setChargen(darstellen(chargen.chargen(produkt)));
        return r;
    }


    private static List<LagerChargeRepresentation> darstellen(List<LagerCharge> liste) {
        return liste.stream()
                .map(c -> new LagerChargeRepresentation(
                        c.getMenge(),
                        c.getPreis() == null ? 0 : c.getPreis().doubleValue(),
                        c.getEingelagertAm()))
                .toList();
    }


    private static double ist(Produkt produkt) {
        return produkt.getLagerbestand() == null || produkt.getLagerbestand().getIstLagerbestand() == null
                ? 0
                : produkt.getLagerbestand().getIstLagerbestand();
    }
}