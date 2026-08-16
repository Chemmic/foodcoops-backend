package de.dhbw.foodcoop.warehouse.plugins.rest;

import de.dhbw.foodcoop.warehouse.adapters.representations.KategorieRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.KategorieToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.RepresentationToKategorieMapper;
import de.dhbw.foodcoop.warehouse.application.lager.KategorieService;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.exceptions.KategorieNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;


@RestController
public class KategorieController {
    private final KategorieService service;
    private final KategorieToRepresentationMapper toRepresentation;
    private final RepresentationToKategorieMapper toKategorie;


    @Autowired
    public KategorieController(KategorieService service, KategorieToRepresentationMapper toRepresentation, RepresentationToKategorieMapper toKategorie) {
        this.service = service;
        this.toRepresentation = toRepresentation;
        this.toKategorie = toKategorie;
    }

    @GetMapping("/kategorien/{id}")
    public KategorieRepresentation one(@PathVariable String id) {
        Kategorie kategorie = service.findById(id)
                .orElseThrow(() -> new KategorieNotFoundException(id));
        return toRepresentation.apply(kategorie);
    }

    @GetMapping("/kategorien")
    public List<KategorieRepresentation> all() {
        List<KategorieRepresentation> kategories = service.all().stream()
                .map(toRepresentation)
                .collect(Collectors.toList());
        return kategories;
    }

    @PostMapping("/kategorien")
    public ResponseEntity<KategorieRepresentation> newKategorie(
            @RequestBody KategorieRepresentation newKategorie) {

        String id = newKategorie.getId() == null
                || newKategorie.getId().isBlank()
                || newKategorie.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newKategorie.getId();

        newKategorie.setId(id);

        Kategorie saved = service.save(toKategorie.apply(newKategorie));

        KategorieRepresentation response = toRepresentation.apply(saved);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PostMapping("/kategorien/{id}")
    public ResponseEntity<KategorieRepresentation> update(
            @RequestBody KategorieRepresentation newKategorie,
            @PathVariable String id) {

        Kategorie oldKategorie = service.findById(id)
                .orElseThrow(() -> new KategorieNotFoundException(id));

        Kategorie updatedKategorie =
                toKategorie.update(oldKategorie, newKategorie);

        Kategorie saved = service.save(updatedKategorie);

        KategorieRepresentation response =
                toRepresentation.apply(saved);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/kategorien/{id}")
    public ResponseEntity<?> delete(@PathVariable String id)  {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }


}
