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
import de.dhbw.foodcoop.warehouse.domain.entities.Deadline;
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
            DeadlineToRepresentationMapper toPresentation) {
        this.service = service;
        this.toDeadline = toDeadline;
        this.toPresentation = toPresentation;
    }

    @GetMapping("/deadline/{id}")
    public DeadlineRepresentation one(@PathVariable String id) {
        Deadline deadline = service.findById(id)
                .orElseThrow(() -> new DeadlineNotFoundException(id));

        return toPresentation.apply(deadline);
    }

    @GetMapping("/deadline/getEndDateOfDeadline/{id}")
    public LocalDateTime getEndDate(@PathVariable String id) {
        Deadline deadline = service.findById(id)
                .orElseThrow(() -> new DeadlineNotFoundException(id));

        return service.calculateDateFromDeadline(deadline);
    }

    @GetMapping("/deadline/lookForUpdate")
    public DeadlineRepresentation update() {
        Optional<Deadline> deadline = service.updateDeadline();

        if (deadline.isEmpty()) {
            return null;
        }

        return toPresentation.apply(deadline.get());
    }

    @GetMapping("/deadline/getByPosition/{id}")
    public DeadlineRepresentation getByPosition(@PathVariable int id) {
        Optional<Deadline> deadline = service.getByPosition(id);

        if (deadline.isEmpty()) {
            return null;
        }

        return toPresentation.apply(deadline.get());
    }

    @GetMapping("/deadline")
    public List<DeadlineRepresentation> all() {
        return service.all().stream()
                .map(toPresentation)
                .collect(Collectors.toList());
    }

    @GetMapping("/deadline/last")
    public DeadlineRepresentation last() {
        Deadline deadline = service.last();

        return toPresentation.apply(deadline);
    }

    @PostMapping("/deadline")
    public ResponseEntity<DeadlineRepresentation> newDeadline(
            @RequestBody DeadlineRepresentation newDeadline) {

        String id = newDeadline.getId() == null
                || newDeadline.getId().isBlank()
                || newDeadline.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newDeadline.getId();

        newDeadline.setId(id);
        newDeadline.setDatum(LocalDateTime.now());

        Deadline saved = service.save(toDeadline.apply(newDeadline));
        DeadlineRepresentation response = toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/deadline/" + response.getId()))
                .body(response);
    }

    @PutMapping("/deadline/{id}")
    public ResponseEntity<DeadlineRepresentation> update(
            @RequestBody DeadlineRepresentation deadline,
            @PathVariable String id) {

        Deadline oldDeadline = service.findById(id)
                .orElseThrow(() -> new DeadlineNotFoundException(id));

        Deadline updatedDeadline = toDeadline.update(oldDeadline, deadline);
        Deadline saved = service.save(updatedDeadline);

        return ResponseEntity.ok(toPresentation.apply(saved));
    }

    @DeleteMapping("/deadline/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws DeadlineInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}