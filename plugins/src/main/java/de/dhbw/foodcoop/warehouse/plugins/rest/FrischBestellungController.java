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

import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestellungRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestellungToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestellungMapper;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestellungService;
import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestellungInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestellungNotFoundException;

@RestController
public class FrischBestellungController {

    private final FrischBestellungService service;
    private final RepresentationToBestellungMapper toFrischBestellung;
    private final BestellungToRepresentationMapper toPresentation;
    private final DeadlineService deadlineService;

    public FrischBestellungController(
            FrischBestellungService service,
            RepresentationToBestellungMapper toFrischBestellung,
            BestellungToRepresentationMapper toPresentation,
            DeadlineService deadlineService) {

        this.service = service;
        this.toFrischBestellung = toFrischBestellung;
        this.toPresentation = toPresentation;
        this.deadlineService = deadlineService;
    }

    @GetMapping("/frischBestellung/{id}")
    public FrischBestellungRepresentation one(@PathVariable String id) {
        FrischBestellung frischBestellung = service.findById(id)
                .orElseThrow(() -> new FrischBestellungNotFoundException(id));

        return (FrischBestellungRepresentation) toPresentation.apply(frischBestellung);
    }

    @GetMapping("/frischBestellung")
    public List<FrischBestellungRepresentation> all() {
        return service.all().stream()
                .map(f -> (FrischBestellungRepresentation) toPresentation.apply(f))
                .collect(Collectors.toList());
    }

    @GetMapping("/frischBestellung/datum/{person_id}")
    public List<FrischBestellungRepresentation> findByDateAfterAndPerson(
            @PathVariable String person_id) {

        Optional<Deadline> deadline = deadlineService.getByPosition(0);

        if (deadline.isEmpty()) {
            return null;
        }

        LocalDateTime datum = deadline.get().getDatum();

        return service.findByDateAfterAndPerson(datum, person_id).stream()
                .map(f -> (FrischBestellungRepresentation) toPresentation.apply(f))
                .collect(Collectors.toList());
    }

    @GetMapping("/frischBestellung/person/{person_id}")
    public List<FrischBestellungRepresentation> findByDateBetween(
            @PathVariable String person_id) {

        Optional<Deadline> date1 = deadlineService.getByPosition(0);
        Optional<Deadline> date2 = deadlineService.getByPosition(1);

        if (date1.isEmpty() || date2.isEmpty()) {
            return null;
        }

        LocalDateTime datum1 = date1.get().getDatum();
        LocalDateTime datum2 = date2.get().getDatum();

        return service.findByDateBetween(datum1, datum2, person_id).stream()
                .map(f -> (FrischBestellungRepresentation) toPresentation.apply(f))
                .collect(Collectors.toList());
    }

    @GetMapping("/frischBestellung/datum/menge")
    public List<FrischBestellungRepresentation> findByDateAfterAndSum() {
        Optional<Deadline> deadline = deadlineService.getByPosition(0);

        if (deadline.isEmpty()) {
            return null;
        }

        LocalDateTime datum = deadline.get().getDatum();

        return service.findByDateAfterAndSum(datum).stream()
                .map(f -> (FrischBestellungRepresentation) toPresentation.apply(f))
                .collect(Collectors.toList());
    }

    @PostMapping("/frischBestellung")
    public ResponseEntity<FrischBestellungRepresentation> newFrischBestellung(
            @RequestBody FrischBestellungRepresentation newFrischBestellung) {

        String id = newFrischBestellung.getId() == null
                || newFrischBestellung.getId().isBlank()
                || newFrischBestellung.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newFrischBestellung.getId();

        newFrischBestellung.setId(id);

        FrischBestellung saved = service.save(
                (FrischBestellung) toFrischBestellung.apply(newFrischBestellung));

        FrischBestellungRepresentation response =
                (FrischBestellungRepresentation) toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/frischBestellung/" + response.getId()))
                .body(response);
    }

    @PutMapping("/frischBestellung/{id}")
    public ResponseEntity<FrischBestellungRepresentation> update(
            @RequestBody FrischBestellungRepresentation changedFrischBestellung,
            @PathVariable String id) {

        FrischBestellung oldFrischBestellung = service.findById(id)
                .orElseThrow(() -> new FrischBestellungNotFoundException(id));

        FrischBestellung updatedFrischBestellung =
                (FrischBestellung) toFrischBestellung.update(
                        oldFrischBestellung,
                        changedFrischBestellung);

        FrischBestellung saved = service.save(updatedFrischBestellung);

        FrischBestellungRepresentation response =
                (FrischBestellungRepresentation) toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/frischBestellung/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws FrischBestellungInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}