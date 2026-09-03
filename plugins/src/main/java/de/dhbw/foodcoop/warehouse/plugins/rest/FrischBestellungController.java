package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestellSummeRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestandToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestellungToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestellungMapper;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestellungService;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestellungInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestellungNotFoundException;

@RestController
public class FrischBestellungController {

    private final FrischBestellungService service;

    private final RepresentationToBestellungMapper
            toFrischBestellung;

    private final BestellungToRepresentationMapper
            toPresentation;

    private final BestandToRepresentationMapper
            bestandToPresentation;

    private final DeadlineService deadlineService;

    public FrischBestellungController(
            FrischBestellungService service,
            RepresentationToBestellungMapper toFrischBestellung,
            BestellungToRepresentationMapper toPresentation,
            BestandToRepresentationMapper bestandToPresentation,
            DeadlineService deadlineService) {

        this.service = service;
        this.toFrischBestellung = toFrischBestellung;
        this.toPresentation = toPresentation;
        this.bestandToPresentation = bestandToPresentation;
        this.deadlineService = deadlineService;
    }

    // -------------------------------------------------------------------------
    // Einzelne Bestellung
    // -------------------------------------------------------------------------

    @GetMapping("/frischBestellung/{id}")
    public FrischBestellungRepresentation one(
            @PathVariable String id) {

        FrischBestellung frischBestellung =
                service.findById(id)
                        .orElseThrow(
                                () -> new FrischBestellungNotFoundException(id)
                        );

        return (FrischBestellungRepresentation)
                toPresentation.apply(frischBestellung);
    }

    // -------------------------------------------------------------------------
    // Alle Bestellungen
    // -------------------------------------------------------------------------

    @GetMapping("/frischBestellung")
    public List<FrischBestellungRepresentation> all() {

        return service.all()
                .stream()
                .map(
                        f ->
                                (FrischBestellungRepresentation)
                                        toPresentation.apply(f)
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Aktuelle Bestellrunde eines Users
    // -------------------------------------------------------------------------

    @GetMapping("/frischBestellung/current/person/{person_id}")
    public List<FrischBestellungRepresentation>
    findCurrentByPerson(
            @PathVariable String person_id) {

        DeadlineEntity deadline =
                getCurrentDeadline();

        return service
                .findByDeadlineAndPerson(
                        deadline.getId(),
                        person_id
                )
                .stream()
                .map(
                        f ->
                                (FrischBestellungRepresentation)
                                        toPresentation.apply(f)
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Komplette Bestellhistorie eines Users
    // -------------------------------------------------------------------------

    @GetMapping("/frischBestellung/person/{person_id}")
    public List<FrischBestellungRepresentation>
    findAllByPerson(
            @PathVariable String person_id) {

        return service
                .findAllByPerson(person_id)
                .stream()
                .map(
                        f ->
                                (FrischBestellungRepresentation)
                                        toPresentation.apply(f)
                )
                .toList();
    }
    @GetMapping(
            "/frischBestellung/previous/person/{person_id}"
    )
    public List<FrischBestellungRepresentation>
    findPreviousByPerson(
            @PathVariable
            String person_id) {

        Optional<DeadlineEntity> deadline =
                deadlineService
                        .getByPosition(1);

        return deadline.map(deadlineEntity -> service
                .findByDeadlineAndPerson(
                        deadlineEntity.getId(),
                        person_id
                )
                .stream()
                .map(
                        f ->
                                (FrischBestellungRepresentation)
                                        toPresentation.apply(f)
                )
                .toList()).orElseGet(List::of);

    }
    // -------------------------------------------------------------------------
    // Summierte Mengen der aktuellen Bestellrunde
    // -------------------------------------------------------------------------

    @GetMapping("/frischBestellung/current/menge")
    public List<FrischBestellSummeRepresentation>
    findCurrentSum() {

        DeadlineEntity deadline =
                getCurrentDeadline();

        List<FrischBestellung> bestellungen =
                service.findAllByDeadline(
                        deadline.getId()
                );

        /*
         * Wir laden echte Bestellungen und gruppieren sie.
         *
         * Keine künstlichen FrischBestellung-Entities
         * mehr über SELECT new FrischBestellung(... SUM ...).
         */
        Map<String, List<FrischBestellung>> gruppiert =
                bestellungen
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        bestellung ->
                                                bestellung
                                                        .getFrischbestand()
                                                        .getId(),

                                        LinkedHashMap::new,

                                        Collectors.toList()
                                )
                        );

        return gruppiert
                .values()
                .stream()
                .map(
                        gruppe -> {

                            FrischBestellung first =
                                    gruppe.getFirst();

                            double summe =
                                    gruppe
                                            .stream()
                                            .mapToDouble(
                                                    FrischBestellung::
                                                            getBestellmenge
                                            )
                                            .sum();

                            FrischBestandRepresentation bestand =
                                    (FrischBestandRepresentation)
                                            bestandToPresentation.apply(
                                                    first.getFrischbestand()
                                            );

                            return new FrischBestellSummeRepresentation(
                                    bestand,
                                    summe
                            );
                        }
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Create
    // -------------------------------------------------------------------------

    @PostMapping("/frischBestellung")
    public ResponseEntity<FrischBestellungRepresentation>
    newFrischBestellung(
            @RequestBody
            FrischBestellungRepresentation newFrischBestellung) {

        String id =
                newFrischBestellung.getId() == null
                        || newFrischBestellung.getId().isBlank()
                        || newFrischBestellung.getId().equals("undefined")

                        ? UUID.randomUUID().toString()
                        : newFrischBestellung.getId();

        newFrischBestellung.setId(id);

        FrischBestellung saved =
                service.save(
                        (FrischBestellung)
                                toFrischBestellung.apply(
                                        newFrischBestellung
                                )
                );

        FrischBestellungRepresentation response =
                (FrischBestellungRepresentation)
                        toPresentation.apply(saved);

        return ResponseEntity
                .created(
                        URI.create(
                                "/frischBestellung/"
                                        + response.getId()
                        )
                )
                .body(response);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @PutMapping("/frischBestellung/{id}")
    public ResponseEntity<FrischBestellungRepresentation>
    update(
            @RequestBody
            FrischBestellungRepresentation changedFrischBestellung,

            @PathVariable
            String id) {

        FrischBestellung oldFrischBestellung =
                service.findById(id)
                        .orElseThrow(
                                () ->
                                        new FrischBestellungNotFoundException(
                                                id
                                        )
                        );

        FrischBestellung updatedFrischBestellung =
                (FrischBestellung)
                        toFrischBestellung.update(
                                oldFrischBestellung,
                                changedFrischBestellung
                        );

        FrischBestellung saved =
                service.save(
                        updatedFrischBestellung
                );

        FrischBestellungRepresentation response =
                (FrischBestellungRepresentation)
                        toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------

    @DeleteMapping("/frischBestellung/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id)
            throws FrischBestellungInUseException {

        service.deleteById(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private DeadlineEntity getCurrentDeadline() {

        /*
         * Falls die vorherige Runde mittlerweile vorbei ist,
         * wird zunächst die neue Deadline erzeugt.
         */
        deadlineService.updateDeadline();

        return deadlineService.last();
    }
}