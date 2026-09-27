package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.util.List;

import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;

public interface LagerChargeRepository {

    /** Chargen eines Produkts, älteste zuerst. */
    List<LagerCharge> vonProdukt(String produktId);

    /** Alle Chargen, älteste zuerst. */
    List<LagerCharge> alle();

    LagerCharge speichern(LagerCharge charge);

    void loeschen(LagerCharge charge);
}
