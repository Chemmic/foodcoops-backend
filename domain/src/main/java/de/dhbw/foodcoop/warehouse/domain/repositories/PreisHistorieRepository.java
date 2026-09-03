package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.util.List;
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
}