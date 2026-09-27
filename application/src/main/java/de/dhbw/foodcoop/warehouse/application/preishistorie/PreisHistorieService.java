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


    // =========================================================================
    // Einzelnen aktuellen Preis speichern
    // =========================================================================

    /**
     * Speichert den Preis eines Bestands für die aktuell
     * gültige Bestellrunde.
     *
     * Existiert für Bestand + Deadline bereits ein Preis,
     * wird dieser aktualisiert.
     *
     * Diese Methode wird beispielsweise aufgerufen, wenn ein
     * Admin während einer laufenden Bestellrunde einen Preis
     * verändert.
     */
    public PreisHistorieEntity speichereAktuellenPreis(
            BestandEntity bestand
    ) {

        /*
         * Nicht darauf verlassen, dass vorher jemand
         * /deadline/lookForUpdate aufgerufen hat.
         *
         * Ist die alte Deadline bereits vorbei, wird hier
         * zunächst automatisch die nächste Runde erzeugt.
         */
        deadlineService.updateDeadline();

        DeadlineEntity deadline =
                deadlineService.last();

        return speicherePreis(
                bestand,
                deadline
        );
    }


    // =========================================================================
    // Einzelnen Preis für konkrete Deadline speichern
    // =========================================================================

    /**
     * Legt einen Preis für Bestand + Deadline an oder
     * aktualisiert einen bereits vorhandenen Eintrag.
     *
     * Diese Methode ist weiterhin sinnvoll für einzelne
     * Preisänderungen.
     *
     * Sie wird NICHT mehr zum Erstellen des kompletten
     * Deadline-Snapshots verwendet.
     */
    public PreisHistorieEntity speicherePreis(
            BestandEntity bestand,
            DeadlineEntity deadline
    ) {

        Optional<PreisHistorieEntity> vorhandenerPreis =
                repository
                        .findeVonBestandUndDeadline(
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


    // =========================================================================
    // Kompletter Snapshot einer neuen Bestellrunde
    // =========================================================================

    /**
     * Erstellt einen Preis-Snapshot aller Bestände für eine
     * neu angelegte Deadline.
     *
     * Wichtig:
     *
     * Das geschieht nicht mehr über:
     *
     *   Bestand laden
     *   -> foreach
     *   -> SELECT
     *   -> INSERT
     *
     * sondern vollständig über ein einziges
     * INSERT ... SELECT auf Datenbankebene.
     *
     * @return Anzahl der neu erzeugten Preis-Einträge
     */
    public int erstelleSnapshotFuerDeadline(
            String deadlineId
    ) {

        return repository
                .erstelleSnapshotFuerDeadline(
                        deadlineId
                );
    }


    // =========================================================================
    // Preis lesen
    // =========================================================================

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
                        () ->
                                new IllegalStateException(
                                        "Kein Preis für Bestand "
                                                + bestandId
                                                + " und Deadline "
                                                + deadlineId
                                                + " vorhanden."
                                )
                )
                .getPreis();
    }


    // =========================================================================
    // Historie lesen
    // =========================================================================

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