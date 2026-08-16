package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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

import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestellungToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestellungMapper;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestellungService;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestellungInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestellungNotFoundException;

@RestController
public class BrotBestellungController {

    private final BrotBestellungService service;
    private final RepresentationToBestellungMapper toBrotBestellung;
    private final BestellungToRepresentationMapper toPresentation;
    private final DeadlineService deadlineService;

    public BrotBestellungController(
            BrotBestellungService service,
            RepresentationToBestellungMapper toBrotBestellung,
            BestellungToRepresentationMapper toPresentation,
            DeadlineService deadlineService) {

        this.service = service;
        this.toBrotBestellung = toBrotBestellung;
        this.toPresentation = toPresentation;
        this.deadlineService = deadlineService;
    }

    @GetMapping("/brotBestellung/{id}")
    public BrotBestellungRepresentation one(@PathVariable String id) {
        BrotBestellung brot = service.findById(id)
                .orElseThrow(() -> new BrotBestellungNotFoundException(id));

        return (BrotBestellungRepresentation) toPresentation.apply(brot);
    }

    @GetMapping("/brotBestellung")
    public List<BrotBestellungRepresentation> all() {
        return service.all().stream()
                .map(b -> (BrotBestellungRepresentation) toPresentation.apply(b))
                .collect(Collectors.toList());
    }

    @GetMapping("/brotBestellung/datum/{person_id}")
    public List<BrotBestellungRepresentation> findByDateAfterAndPerson(
            @PathVariable String person_id) {

        Optional<Deadline> deadline = deadlineService.getByPosition(0);

        if (deadline.isEmpty()) {
            return null;
        }

        LocalDateTime datum = deadline.get().getDatum();

        return service.findByDateAfterAndPerson(datum, person_id).stream()
                .map(b -> (BrotBestellungRepresentation) toPresentation.apply(b))
                .collect(Collectors.toList());
    }

    @GetMapping("/brotBestellung/person/{person_id}")
    public List<BrotBestellungRepresentation> findByDateBetween(
            @PathVariable String person_id) {

        Optional<Deadline> date1 = deadlineService.getByPosition(0);
        Optional<Deadline> date2 = deadlineService.getByPosition(1);

        if (date1.isEmpty()) {
            return null;
        }

        if (date2.isEmpty()) {
            return findByDateAfterAndPerson(person_id);
        }

        LocalDateTime datum1 = date1.get().getDatum();
        LocalDateTime datum2 = date2.get().getDatum();

        return service.findByDateBetween(datum1, datum2, person_id).stream()
                .map(b -> (BrotBestellungRepresentation) toPresentation.apply(b))
                .collect(Collectors.toList());
    }

    @GetMapping("/brotBestellung/datum/menge")
    public List<BrotBestellungRepresentation> findByDateAfterAndSum() {

        Optional<Deadline> deadline = deadlineService.getByPosition(0);

        if (deadline.isEmpty()) {
            return null;
        }

        LocalDateTime datum = deadline.get().getDatum();

        return service.findByDateAfterAndSum(datum).stream()
                .map(b -> (BrotBestellungRepresentation) toPresentation.apply(b))
                .collect(Collectors.toList());
    }

    @PostMapping("/brotBestellung")
    public ResponseEntity<BrotBestellungRepresentation> newBrotBestellung(
            @RequestBody BrotBestellungRepresentation newBrotBestellung) {

        String id = newBrotBestellung.getId() == null
                || newBrotBestellung.getId().isBlank()
                || newBrotBestellung.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newBrotBestellung.getId();

        newBrotBestellung.setId(id);

        BrotBestellung saved = service.save(
                (BrotBestellung) toBrotBestellung.apply(newBrotBestellung));

        BrotBestellungRepresentation response =
                (BrotBestellungRepresentation) toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/brotBestellung/" + response.getId()))
                .body(response);
    }

    @PutMapping("/brotBestellung/{id}")
    public ResponseEntity<BrotBestellungRepresentation> update(
            @RequestBody BrotBestellungRepresentation brotBestellung,
            @PathVariable String id) {

        BrotBestellung oldBrotBestellung = service.findById(id)
                .orElseThrow(() -> new BrotBestellungNotFoundException(id));

        BrotBestellung updatedBrotBestellung =
                (BrotBestellung) toBrotBestellung.update(
                        oldBrotBestellung,
                        brotBestellung);

        BrotBestellung saved = service.save(updatedBrotBestellung);

        BrotBestellungRepresentation response =
                (BrotBestellungRepresentation) toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/brotBestellung/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws BrotBestellungInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}