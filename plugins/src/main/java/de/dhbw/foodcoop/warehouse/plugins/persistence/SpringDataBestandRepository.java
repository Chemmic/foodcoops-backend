package de.dhbw.foodcoop.warehouse.plugins.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;

public interface SpringDataBestandRepository
        extends JpaRepository<BestandEntity, String> {
}