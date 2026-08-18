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

    public ProduktController(
            ProduktService service,
            RepresentationToBestandMapper toProdukt,
            BestandToRepresentationMapper toPresentation) {

        this.service = service;
        this.toProdukt = toProdukt;
        this.toPresentation = toPresentation;
    }

    @GetMapping("/produkte/{id}")
    public ProduktRepresentation one(@PathVariable String id) {
        Produkt produkt = service.findById(id)
                .orElseThrow(() -> new ProduktNotFoundException(id));

        return (ProduktRepresentation) toPresentation.apply(produkt);
    }

    @GetMapping("/produkte")
    public List<ProduktRepresentation> all() {
        return service.all().stream()
                .map(produkt ->
                        (ProduktRepresentation) toPresentation.apply(produkt))
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

        ProduktRepresentation response =
                (ProduktRepresentation) toPresentation.apply(saved);

        return ResponseEntity
                .created(URI.create("/produkte/" + response.getId()))
                .body(response);
    }

    @PutMapping("/produkte/{id}")
    public ResponseEntity<ProduktRepresentation> update(
            @RequestBody ProduktRepresentation changedProdukt,
            @PathVariable String id) {

        Produkt oldProdukt = service.findById(id)
                .orElseThrow(() -> new ProduktNotFoundException(id));

        Produkt updatedProdukt =
                (Produkt) toProdukt.update(
                        oldProdukt,
                        changedProdukt);

        Produkt saved = service.save(updatedProdukt);

        ProduktRepresentation response =
                (ProduktRepresentation) toPresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/produkte/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws ProduktInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}