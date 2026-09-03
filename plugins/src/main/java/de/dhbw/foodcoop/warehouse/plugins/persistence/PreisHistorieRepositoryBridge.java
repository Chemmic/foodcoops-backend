package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;
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
        return repository.save(preisHistorie);
    }

    @Override
    public Optional<PreisHistorieEntity> findeMitId(
            String id
    ) {
        return repository.findById(id);
    }

    @Override
    public Optional<PreisHistorieEntity> findeVonBestandUndDeadline(
            String bestandId,
            String deadlineId
    ) {
        return repository.findByBestand_IdAndDeadline_Id(
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
        return repository.findAllByDeadline_Id(
                deadlineId
        );
    }
}