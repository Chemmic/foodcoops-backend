package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.PreisHistorieRepository;

@Repository
public class PreisHistorieRepositoryBridge
        implements PreisHistorieRepository {

    private final SpringDataPreisHistorieRepository repository;

    public PreisHistorieRepositoryBridge(
            SpringDataPreisHistorieRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public PreisHistorieEntity speichern(
            PreisHistorieEntity preisHistorie
    ) {
        return repository.save(
                preisHistorie
        );
    }

    @Override
    public Optional<PreisHistorieEntity> findeMitId(
            String id
    ) {
        return repository.findById(
                id
        );
    }

    @Override
    public Optional<PreisHistorieEntity>
    findeVonBestandUndDeadline(
            String bestandId,
            String deadlineId
    ) {

        return repository
                .findByBestand_IdAndDeadline_Id(
                        bestandId,
                        deadlineId
                );
    }

    @Override
    public List<PreisHistorieEntity> findeAlleVonBestand(
            String bestandId
    ) {

        return repository
                .findAllByBestand_IdOrderByDeadline_DatumAsc(
                        bestandId
                );
    }

    @Override
    public List<PreisHistorieEntity> findeAlleVonDeadline(
            String deadlineId
    ) {

        return repository
                .findAllByDeadline_Id(
                        deadlineId
                );
    }

    @Override
    public Map<String, BigDecimal> findePreiseVonDeadline(
            String deadlineId
    ) {
        Map<String, BigDecimal> preise = new HashMap<>();

        for (Object[] zeile : repository.findePreiseVonDeadline(deadlineId)) {
            if (zeile[0] != null && zeile[1] != null) {
                preise.put((String) zeile[0], (BigDecimal) zeile[1]);
            }
        }

        return preise;
    }

    @Override
    public Map<LocalDateTime, BigDecimal> findePreisverlaufVonBestand(
            String bestandId
    ) {
        Map<LocalDateTime, BigDecimal> verlauf = new LinkedHashMap<>();

        for (Object[] zeile : repository.findePreisverlaufVonBestand(bestandId)) {
            if (zeile[0] != null && zeile[1] != null) {
                verlauf.put((LocalDateTime) zeile[0], (BigDecimal) zeile[1]);
            }
        }

        return verlauf;
    }

    @Override
    public int erstelleSnapshotFuerDeadline(
            String deadlineId
    ) {

        return repository
                .erstelleSnapshotFuerDeadline(
                        deadlineId
                );
    }
}