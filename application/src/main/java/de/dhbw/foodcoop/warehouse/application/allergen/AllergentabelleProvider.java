package de.dhbw.foodcoop.warehouse.application.allergen;

import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;

import java.util.Map;
import java.util.Optional;

public interface AllergentabelleProvider {

    /**
     * Gibt neue Allergendaten zurück, falls eine neue
     * Allergentabelle verfügbar war.
     *
     * Optional.empty() = keine neue Tabelle.
     */
    Optional<Map<String, AllergenInfo>> loadIfUpdated();
}