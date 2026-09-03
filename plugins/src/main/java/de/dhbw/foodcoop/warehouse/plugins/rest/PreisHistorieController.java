package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.adapters.representations.PreisHistorieRepresentation;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;

@RestController
@RequestMapping(
        "/preisHistorie"
)
public class PreisHistorieController {

    private final PreisHistorieService service;
    private final DeadlineService deadlineService;

    public PreisHistorieController(
            PreisHistorieService service,
            DeadlineService deadlineService
    ) {
        this.service = service;
        this.deadlineService =
                deadlineService;
    }

    @GetMapping(
            "/bestand/{bestandId}"
    )
    public List<PreisHistorieRepresentation>
    historieVonBestand(
            @PathVariable
            String bestandId
    ) {

        return service
                .historieVonBestand(
                        bestandId
                )
                .stream()
                .map(
                        preisHistorie ->
                                new PreisHistorieRepresentation(
                                        preisHistorie.getId(),
                                        preisHistorie
                                                .getBestand()
                                                .getId(),

                                        preisHistorie
                                                .getBestand()
                                                .getName(),

                                        preisHistorie
                                                .getDeadline()
                                                .getId(),

                                        deadlineService
                                                .calculateDateFromDeadline(
                                                        preisHistorie
                                                                .getDeadline()
                                                ),

                                        preisHistorie
                                                .getPreis()
                                )
                )
                .toList();
    }
}