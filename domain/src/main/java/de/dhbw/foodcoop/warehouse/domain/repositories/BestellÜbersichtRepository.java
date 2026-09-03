package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.util.Optional;

import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;

public interface BestellÜbersichtRepository {
	BestellUebersicht findeMitDeadline(DeadlineEntity deadline);

    BestellUebersicht speichern(BestellUebersicht bestellÜbersicht);

    Optional<BestellUebersicht> findeMitId(String id);

    void deleteById(String id);
}
