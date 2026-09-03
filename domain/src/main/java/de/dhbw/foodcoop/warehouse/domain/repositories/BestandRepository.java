package de.dhbw.foodcoop.warehouse.domain.repositories;

import java.util.List;

import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;

public interface BestandRepository {

    List<BestandEntity> alle();
}