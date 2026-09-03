package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestandRepository;

@Repository
public class BestandRepositoryBridge
        implements BestandRepository {

    private final SpringDataBestandRepository repository;

    public BestandRepositoryBridge(
            SpringDataBestandRepository repository
    ) {
        this.repository = repository;
    }

    @Override
    public List<BestandEntity> alle() {
        return repository.findAll();
    }
}