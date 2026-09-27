package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;

public interface PreisHistorieRepository {

    PreisHistorieEntity speichern(
            PreisHistorieEntity preisHistorie
    );

    Optional<PreisHistorieEntity> findeMitId(
            String id
    );

    Optional<PreisHistorieEntity> findeVonBestandUndDeadline(
            String bestandId,
            String deadlineId
    );

    List<PreisHistorieEntity> findeAlleVonBestand(
            String bestandId
    );

    List<PreisHistorieEntity> findeAlleVonDeadline(
            String deadlineId
    );

    /**
     * Nur Bestand-ID -> Preis einer Deadline, ohne die Bestände selbst
     * zu laden (schnell, eine einzige Abfrage).
     */
    Map<String, BigDecimal> findePreiseVonDeadline(
            String deadlineId
    );

    /**
     * Preisverlauf eines Bestands: Deadline-Datum -> Preis,
     * aufsteigend sortiert.
     */
    Map<LocalDateTime, BigDecimal> findePreisverlaufVonBestand(
            String bestandId
    );

    /**
     * Erstellt für alle Bestände einen Preis-Snapshot
     * für die angegebene Deadline.
     *
     * Die Implementierung geschieht als ein einziges
     * INSERT ... SELECT auf Datenbankebene.
     *
     * @return Anzahl der neu angelegten Preis-Einträge
     */
    int erstelleSnapshotFuerDeadline(
            String deadlineId
    );
}