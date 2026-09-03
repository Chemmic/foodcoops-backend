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

import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestellSummeRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestandToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestellungToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestellungMapper;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestellungService;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestellungInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestellungNotFoundException;

@RestController
public class BrotBestellungController {

    private final BrotBestellungService service;

    private final RepresentationToBestellungMapper
            toBrotBestellung;

    private final BestellungToRepresentationMapper
            toPresentation;

    private final BestandToRepresentationMapper
            bestandToPresentation;

    private final DeadlineService deadlineService;

    public BrotBestellungController(
            BrotBestellungService service,
            RepresentationToBestellungMapper toBrotBestellung,
            BestellungToRepresentationMapper toPresentation,
            BestandToRepresentationMapper bestandToPresentation,
            DeadlineService deadlineService) {

        this.service = service;
        this.toBrotBestellung = toBrotBestellung;
        this.toPresentation = toPresentation;
        this.bestandToPresentation = bestandToPresentation;
        this.deadlineService = deadlineService;
    }

    // -------------------------------------------------------------------------
    // Einzelne Bestellung
    // -------------------------------------------------------------------------

    @GetMapping("/brotBestellung/{id}")
    public BrotBestellungRepresentation one(
            @PathVariable String id) {

        BrotBestellung brotBestellung =
                service.findById(id)
                        .orElseThrow(
                                () ->
                                        new BrotBestellungNotFoundException(
                                                id
                                        )
                        );

        return (BrotBestellungRepresentation)
                toPresentation.apply(
                        brotBestellung
                );
    }

    // -------------------------------------------------------------------------
    // Alle Bestellungen
    // -------------------------------------------------------------------------

    @GetMapping("/brotBestellung")
    public List<BrotBestellungRepresentation> all() {

        return service.all()
                .stream()
                .map(
                        b ->
                                (BrotBestellungRepresentation)
                                        toPresentation.apply(b)
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Aktuelle Bestellrunde eines Users
    // -------------------------------------------------------------------------

    @GetMapping("/brotBestellung/current/person/{person_id}")
    public List<BrotBestellungRepresentation>
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
                        b ->
                                (BrotBestellungRepresentation)
                                        toPresentation.apply(b)
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Komplette Bestellhistorie eines Users
    // -------------------------------------------------------------------------

    @GetMapping("/brotBestellung/person/{person_id}")
    public List<BrotBestellungRepresentation>
    findAllByPerson(
            @PathVariable String person_id) {

        return service
                .findAllByPerson(person_id)
                .stream()
                .map(
                        b ->
                                (BrotBestellungRepresentation)
                                        toPresentation.apply(b)
                )
                .toList();
    }

    // -------------------------------------------------------------------------
    // Summierte Mengen der aktuellen Bestellrunde
    // -------------------------------------------------------------------------

    @GetMapping("/brotBestellung/current/menge")
    public List<BrotBestellSummeRepresentation>
    findCurrentSum() {

        DeadlineEntity deadline =
                getCurrentDeadline();

        List<BrotBestellung> bestellungen =
                service.findAllByDeadline(
                        deadline.getId()
                );

        Map<String, List<BrotBestellung>> gruppiert =
                bestellungen
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        bestellung ->
                                                bestellung
                                                        .getBrotBestand()
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

                            BrotBestellung first =
                                    gruppe.getFirst();

                            double summe =
                                    gruppe
                                            .stream()
                                            .mapToDouble(
                                                    BrotBestellung::
                                                            getBestellmenge
                                            )
                                            .sum();

                            BrotBestandRepresentation bestand =
                                    (BrotBestandRepresentation)
                                            bestandToPresentation.apply(
                                                    first.getBrotBestand()
                                            );

                            return new BrotBestellSummeRepresentation(
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

    @PostMapping("/brotBestellung")
    public ResponseEntity<BrotBestellungRepresentation>
    newBrotBestellung(
            @RequestBody
            BrotBestellungRepresentation newBrotBestellung) {

        String id =
                newBrotBestellung.getId() == null
                        || newBrotBestellung.getId().isBlank()
                        || newBrotBestellung.getId().equals("undefined")

                        ? UUID.randomUUID().toString()
                        : newBrotBestellung.getId();

        newBrotBestellung.setId(id);

        BrotBestellung saved =
                service.save(
                        (BrotBestellung)
                                toBrotBestellung.apply(
                                        newBrotBestellung
                                )
                );

        BrotBestellungRepresentation response =
                (BrotBestellungRepresentation)
                        toPresentation.apply(saved);

        return ResponseEntity
                .created(
                        URI.create(
                                "/brotBestellung/"
                                        + response.getId()
                        )
                )
                .body(response);
    }

    // -------------------------------------------------------------------------
    // Update
    // -------------------------------------------------------------------------

    @PutMapping("/brotBestellung/{id}")
    public ResponseEntity<BrotBestellungRepresentation>
    update(
            @RequestBody
            BrotBestellungRepresentation changedBrotBestellung,

            @PathVariable
            String id) {

        BrotBestellung oldBrotBestellung =
                service.findById(id)
                        .orElseThrow(
                                () ->
                                        new BrotBestellungNotFoundException(
                                                id
                                        )
                        );

        BrotBestellung updatedBrotBestellung =
                (BrotBestellung)
                        toBrotBestellung.update(
                                oldBrotBestellung,
                                changedBrotBestellung
                        );

        BrotBestellung saved =
                service.save(
                        updatedBrotBestellung
                );

        BrotBestellungRepresentation response =
                (BrotBestellungRepresentation)
                        toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    // -------------------------------------------------------------------------
    // Delete
    // -------------------------------------------------------------------------
    @GetMapping(
            "/brotBestellung/previous/person/{person_id}"
    )
    public List<BrotBestellungRepresentation>
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
                        b ->
                                (BrotBestellungRepresentation)
                                        toPresentation.apply(b)
                )
                .toList()).orElseGet(List::of);

    }
    @DeleteMapping("/brotBestellung/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id)
            throws BrotBestellungInUseException {

        service.deleteById(id);

        return ResponseEntity
                .noContent()
                .build();
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private DeadlineEntity getCurrentDeadline() {

        deadlineService.updateDeadline();

        return deadlineService.last();
    }
}