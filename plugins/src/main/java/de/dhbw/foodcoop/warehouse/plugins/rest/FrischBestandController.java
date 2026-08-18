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

import de.dhbw.foodcoop.warehouse.adapters.representations.FrischBestandRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.BestandToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToBestandMapper;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestandService;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestandInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.FrischBestandNotFoundException;

@RestController
public class FrischBestandController {

    private final FrischBestandService service;
    private final RepresentationToBestandMapper toFrischBestand;
    private final BestandToRepresentationMapper toPresentation;

    public FrischBestandController(
            FrischBestandService service,
            RepresentationToBestandMapper toFrischBestand,
            BestandToRepresentationMapper toPresentation) {

        this.service = service;
        this.toFrischBestand = toFrischBestand;
        this.toPresentation = toPresentation;
    }

    @GetMapping("/frischBestand/{id}")
    public FrischBestandRepresentation one(@PathVariable String id) {
        FrischBestand frischBestand = service.findById(id)
                .orElseThrow(() -> new FrischBestandNotFoundException(id));

        return (FrischBestandRepresentation) toPresentation.apply(frischBestand);
    }

    @GetMapping("/frischBestand")
    public List<FrischBestandRepresentation> all() {
        return service.all().stream()
                .map(f -> (FrischBestandRepresentation) toPresentation.apply(f))
                .collect(Collectors.toList());
    }

    @PostMapping("/frischBestand")
    public ResponseEntity<FrischBestandRepresentation> newFrischBestand(
            @RequestBody FrischBestandRepresentation newFrischBestand) {

        String id = newFrischBestand.getId() == null
                || newFrischBestand.getId().isBlank()
                || newFrischBestand.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newFrischBestand.getId();

        newFrischBestand.setId(id);

        FrischBestand saved = service.save(
                (FrischBestand) toFrischBestand.apply(newFrischBestand));

        FrischBestandRepresentation response =
                (FrischBestandRepresentation) toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/frischBestand/" + response.getId()))
                .body(response);
    }

    @PutMapping("/frischBestand/{id}")
    public ResponseEntity<FrischBestandRepresentation> update(
            @RequestBody FrischBestandRepresentation changedProdukt,
            @PathVariable String id) {

        FrischBestand oldFrischBestand = service.findById(id)
                .orElseThrow(() -> new FrischBestandNotFoundException(id));

        FrischBestand updatedFrischBestand =
                (FrischBestand) toFrischBestand.update(
                        oldFrischBestand,
                        changedProdukt);

        FrischBestand saved = service.save(updatedFrischBestand);

        FrischBestandRepresentation response =
                (FrischBestandRepresentation) toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/frischBestand/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws FrischBestandInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}