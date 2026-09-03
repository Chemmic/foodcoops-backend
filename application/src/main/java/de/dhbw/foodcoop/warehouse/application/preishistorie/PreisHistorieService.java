package de.dhbw.foodcoop.warehouse.application.preishistorie;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.PreisHistorieRepository;

@Service
public class PreisHistorieService {

    private final PreisHistorieRepository repository;
    private final DeadlineService deadlineService;

    public PreisHistorieService(
            PreisHistorieRepository repository,
            DeadlineService deadlineService
    ) {
        this.repository = repository;
        this.deadlineService = deadlineService;
    }

    /**
     * Speichert den Preis eines Bestands für die aktuell
     * gültige Bestellrunde.
     *
     * Existiert für Bestand + Deadline bereits ein Preis,
     * wird dieser aktualisiert.
     */
    public PreisHistorieEntity speichereAktuellenPreis(
            BestandEntity bestand
    ) {

        /*
         * Wichtig:
         * Nicht darauf verlassen, dass das Frontend vorher
         * /deadline/lookForUpdate aufgerufen hat.
         *
         * Wenn die alte Deadline bereits vorbei ist,
         * wird hier zuerst eine neue Runde erzeugt.
         */
        deadlineService.updateDeadline();

        DeadlineEntity deadline =
                deadlineService.last();

        return speicherePreis(
                bestand,
                deadline
        );
    }

    /**
     * Legt einen Snapshot für Bestand + Deadline an
     * oder aktualisiert den vorhandenen Snapshot.
     */
    public PreisHistorieEntity speicherePreis(
            BestandEntity bestand,
            DeadlineEntity deadline
    ) {

        Optional<PreisHistorieEntity> vorhandenerPreis =
                repository.findeVonBestandUndDeadline(
                        bestand.getId(),
                        deadline.getId()
                );

        if (vorhandenerPreis.isPresent()) {

            PreisHistorieEntity preisHistorie =
                    vorhandenerPreis.get();

            preisHistorie.setPreis(
                    bestand.getPreis()
            );

            return repository.speichern(
                    preisHistorie
            );
        }

        PreisHistorieEntity preisHistorie =
                new PreisHistorieEntity(
                        UUID.randomUUID().toString(),
                        bestand,
                        deadline,
                        bestand.getPreis()
                );

        return repository.speichern(
                preisHistorie
        );
    }

    public Optional<PreisHistorieEntity> findePreis(
            String bestandId,
            String deadlineId
    ) {
        return repository
                .findeVonBestandUndDeadline(
                        bestandId,
                        deadlineId
                );
    }

    public BigDecimal preisFuer(
            String bestandId,
            String deadlineId
    ) {
        return repository
                .findeVonBestandUndDeadline(
                        bestandId,
                        deadlineId
                )
                .orElseThrow(
                        () -> new IllegalStateException(
                                "Kein Preis für Bestand "
                                        + bestandId
                                        + " und Deadline "
                                        + deadlineId
                                        + " vorhanden."
                        )
                )
                .getPreis();
    }

    public List<PreisHistorieEntity> historieVonBestand(
            String bestandId
    ) {
        return repository
                .findeAlleVonBestand(
                        bestandId
                );
    }

    public List<PreisHistorieEntity> preiseVonDeadline(
            String deadlineId
    ) {
        return repository
                .findeAlleVonDeadline(
                        deadlineId
                );
    }
}