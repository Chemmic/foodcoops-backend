package de.dhbw.foodcoop.warehouse.plugins.rest;

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

import de.dhbw.foodcoop.warehouse.adapters.representations.BrotBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestandToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestandMapper;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestandService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestandInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.BrotBestandNotFoundException;

@RestController
public class BrotBestandController {

    private final BrotBestandService service;
    private final RepresentationToBestandMapper toBrotBestand;
    private final BestandToRepresentationMapper toPresentation;

    public BrotBestandController(
            BrotBestandService service,
            RepresentationToBestandMapper toBrotBestand,
            BestandToRepresentationMapper toPresentation) {

        this.service = service;
        this.toBrotBestand = toBrotBestand;
        this.toPresentation = toPresentation;
    }

    @GetMapping("/brotBestand/{id}")
    public BrotBestandRepresentation one(@PathVariable String id) {
        BrotBestand brotBestand = service.findById(id)
                .orElseThrow(() -> new BrotBestandNotFoundException(id));

        return (BrotBestandRepresentation) toPresentation.apply(brotBestand);
    }

    @GetMapping("/brotBestand")
    public List<BrotBestandRepresentation> all() {
        return service.all().stream()
                .map(brotBestand ->
                        (BrotBestandRepresentation) toPresentation.apply(brotBestand))
                .collect(Collectors.toList());
    }

    @PostMapping("/brotBestand")
    public ResponseEntity<BrotBestandRepresentation> newBrotBestand(
            @RequestBody BrotBestandRepresentation newBrotBestand) {

        String id = newBrotBestand.getId() == null
                || newBrotBestand.getId().isBlank()
                || newBrotBestand.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newBrotBestand.getId();

        newBrotBestand.setId(id);

        BrotBestand saved = service.save(
                (BrotBestand) toBrotBestand.apply(newBrotBestand));

        BrotBestandRepresentation response =
                (BrotBestandRepresentation) toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/brotBestand/" + response.getId()))
                .body(response);
    }

    @PutMapping("/brotBestand/{id}")
    public ResponseEntity<BrotBestandRepresentation> update(
            @RequestBody BrotBestandRepresentation changedBrotBestand,
            @PathVariable String id) {

        BrotBestand oldBrotBestand = service.findById(id)
                .orElseThrow(() -> new BrotBestandNotFoundException(id));

        BrotBestand updatedBrotBestand =
                (BrotBestand) toBrotBestand.update(
                        oldBrotBestand,
                        changedBrotBestand);

        BrotBestand saved = service.save(updatedBrotBestand);

        BrotBestandRepresentation response =
                (BrotBestandRepresentation) toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/brotBestand/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws BrotBestandInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}