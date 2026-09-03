package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;
import java.util.Optional;

import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import de.dhbw.foodcoop.warehouse.domain.entities.PreisHistorieEntity;

public interface SpringDataPreisHistorieRepository
        extends JpaRepository<PreisHistorieEntity, String> {

    Optional<PreisHistorieEntity> findByBestand_IdAndDeadline_Id(
            String bestandId,
            String deadlineId
    );

    List<PreisHistorieEntity> findAllByBestand_IdOrderByDeadline_DatumAsc(
            String bestandId
    );

    List<PreisHistorieEntity> findAllByDeadline_Id(
            String deadlineId
    );
}