package de.dhbw.foodcoop.warehouse.domain.repositories;

import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;

import java.util.List;
import java.util.Optional;

public interface DeadlineRepository {
    List<DeadlineEntity> alle();

    Optional<DeadlineEntity> letzte();
    
    DeadlineEntity speichern(DeadlineEntity deadline);

    Optional<DeadlineEntity> findeMitId(String id);

    void deleteById(String id);
    
    Optional<DeadlineEntity> findeNachReihenfolge(int position);
}
