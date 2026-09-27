package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.DeadlineRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.DeadlineToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToDeadlineMapper;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.exceptions.DeadlineInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.DeadlineNotFoundException;

@RestController
public class DeadlineController {

    private final DeadlineService service;
    private final RepresentationToDeadlineMapper toDeadline;
    private final DeadlineToRepresentationMapper toPresentation;

    @Autowired
    public DeadlineController(
            DeadlineService service,
            RepresentationToDeadlineMapper toDeadline,
            DeadlineToRepresentationMapper toPresentation
    ) {
        this.service = service;
        this.toDeadline = toDeadline;
        this.toPresentation = toPresentation;
    }


    // =========================================================================
    // Eine Deadline
    // =========================================================================

    @GetMapping("/deadline/{id}")
    public DeadlineRepresentation one(
            @PathVariable String id
    ) {

        DeadlineEntity deadline =
                service
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DeadlineNotFoundException(
                                                id
                                        )
                        );

        return toPresentation.apply(
                deadline
        );
    }


    // =========================================================================
    // Tatsächliches Deadline-Datum
    // =========================================================================

    @GetMapping(
            "/deadline/getEndDateOfDeadline/{id}"
    )
    public LocalDateTime getEndDate(
            @PathVariable String id
    ) {

        DeadlineEntity deadline =
                service
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DeadlineNotFoundException(
                                                id
                                        )
                        );

        return service
                .calculateDateFromDeadline(
                        deadline
                );
    }


    // =========================================================================
    // Prüfen, ob neue Deadline benötigt wird
    // =========================================================================

    @GetMapping(
            "/deadline/lookForUpdate"
    )
    public DeadlineRepresentation update() {

        Optional<DeadlineEntity> deadline =
                service.updateDeadline();

        if (deadline.isEmpty()) {
            return null;
        }

        return toPresentation.apply(
                deadline.get()
        );
    }


    // =========================================================================
    // Deadline nach Position
    // =========================================================================

    @GetMapping(
            "/deadline/getByPosition/{id}"
    )
    public DeadlineRepresentation getByPosition(
            @PathVariable int id
    ) {

        Optional<DeadlineEntity> deadline =
                service.getByPosition(
                        id
                );

        if (deadline.isEmpty()) {
            return null;
        }

        return toPresentation.apply(
                deadline.get()
        );
    }


    // =========================================================================
    // Alle Deadlines
    // =========================================================================

    @GetMapping("/deadline")
    public List<DeadlineRepresentation> all() {

        return service
                .all()
                .stream()
                .map(
                        toPresentation
                )
                .collect(
                        Collectors.toList()
                );
    }


    // =========================================================================
    // Letzte Deadline
    // =========================================================================

    @GetMapping("/deadline/last")
    public DeadlineRepresentation last() {

        DeadlineEntity deadline =
                service.last();

        return toPresentation.apply(
                deadline
        );
    }


    // =========================================================================
    // Neue Deadline
    // =========================================================================

    @PostMapping("/deadline")
    public ResponseEntity<DeadlineRepresentation>
    newDeadline(
            @RequestBody
            DeadlineRepresentation newDeadline
    ) {

        String id =
                newDeadline.getId() == null
                        || newDeadline.getId().isBlank()
                        || newDeadline
                        .getId()
                        .equals("undefined")

                        ? UUID.randomUUID().toString()
                        : newDeadline.getId();

        newDeadline.setId(
                id
        );

        newDeadline.setDatum(
                LocalDateTime.now()
        );

        /*
         * Neue Deadline:
         *
         * save() veröffentlicht DeadlineSavedEvent.
         */
        DeadlineEntity saved =
                service.save(
                        toDeadline.apply(
                                newDeadline
                        )
                );

        DeadlineRepresentation response =
                toPresentation.apply(
                        saved
                );

        return ResponseEntity
                .created(
                        URI.create(
                                "/deadline/"
                                        + response.getId()
                        )
                )
                .body(
                        response
                );
    }


    // =========================================================================
    // Bestehende Deadline bearbeiten
    // =========================================================================

    @PutMapping("/deadline/{id}")
    public ResponseEntity<DeadlineRepresentation>
    update(
            @RequestBody
            DeadlineRepresentation deadline,

            @PathVariable
            String id
    ) {

        DeadlineEntity oldDeadline =
                service
                        .findById(id)
                        .orElseThrow(
                                () ->
                                        new DeadlineNotFoundException(
                                                id
                                        )
                        );

        DeadlineEntity updatedDeadline =
                toDeadline.update(
                        oldDeadline,
                        deadline
                );

        /*
         * WICHTIG:
         *
         * Kein service.save() mehr!
         *
         * Sonst würde ein PUT wieder:
         *
         * - Preis-Snapshot
         * - Bestellübersicht
         *
         * triggern.
         */
        DeadlineEntity saved =
                service.update(
                        updatedDeadline
                );

        return ResponseEntity.ok(
                toPresentation.apply(
                        saved
                )
        );
    }


    // =========================================================================
    // Löschen
    // =========================================================================

    @DeleteMapping("/deadline/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id
    ) throws DeadlineInUseException {

        service.deleteById(
                id
        );

        return ResponseEntity
                .noContent()
                .build();
    }


    // =========================================================================
    // Debug
    // =========================================================================

    @PostMapping(
            "/deadline/debug/expire"
    )
    public ResponseEntity<DeadlineRepresentation>
    expireDeadline() {

        System.out.println(
                "[DeadlineController] Erzwinge neue Deadline..."
        );

        DeadlineEntity deadline =
                service.forceNextDeadline();

        return ResponseEntity.ok(
                toPresentation.apply(
                        deadline
                )
        );
    }
}