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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.EinheitRepresentation;
import de.dhbw.foodcoop.warehouse.adapters.representations.mappers.EinheitToRepresentationMapper;
import de.dhbw.foodcoop.warehouse.application.lager.EinheitService;
import de.dhbw.foodcoop.warehouse.domain.exceptions.EinheitInUseException;
import de.dhbw.foodcoop.warehouse.domain.exceptions.EinheitNotFoundException;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;

@RestController
public class EinheitController {

    private final EinheitService service;
    private final EinheitToRepresentationMapper toRepresentationMapper;

    public EinheitController(
            EinheitService service,
            EinheitToRepresentationMapper toRepresentationMapper) {

        this.service = service;
        this.toRepresentationMapper = toRepresentationMapper;
    }

    @GetMapping("/einheiten/{id}")
    public EinheitRepresentation one(@PathVariable String id) {
        Einheit einheit = service.findById(id)
                .orElseThrow(() -> new EinheitNotFoundException(id));

        return toRepresentationMapper.apply(einheit);
    }

    @GetMapping("/einheiten")
    public List<EinheitRepresentation> all() {
        return service.all().stream()
                .map(toRepresentationMapper)
                .collect(Collectors.toList());
    }

    @PostMapping("/einheiten")
    public ResponseEntity<EinheitRepresentation> newEinheit(
            @RequestBody EinheitRepresentation newEinheit) {

        String id = newEinheit.getId() == null
                || newEinheit.getId().isBlank()
                || newEinheit.getId().equals("undefined")
                ? UUID.randomUUID().toString()
                : newEinheit.getId();

        Einheit withId = new Einheit(id, newEinheit.getName());
        Einheit saved = service.save(withId);

        EinheitRepresentation response =
                toRepresentationMapper.apply(saved);

        return ResponseEntity
                .created(URI.create("/einheiten/" + response.getId()))
                .body(response);
    }

    @DeleteMapping("/einheiten/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id)
            throws EinheitInUseException {

        service.deleteById(id);

        return ResponseEntity.noContent().build();
    }
}