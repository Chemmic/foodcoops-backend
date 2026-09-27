package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;

import org.springframework.stereotype.Repository;

import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;
import de.dhbw.foodcoop.warehouse.domain.repositories.LagerChargeRepository;

@Repository
public class LagerChargeRepositoryBridge implements LagerChargeRepository {

    private final SpringDataLagerChargeRepository repository;

    public LagerChargeRepositoryBridge(SpringDataLagerChargeRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<LagerCharge> vonProdukt(String produktId) {
        return repository.findByProduktIdOrderByEingelagertAmAscIdAsc(produktId);
    }

    @Override
    public List<LagerCharge> alle() {
        return repository.findAllByOrderByEingelagertAmAscIdAsc();
    }

    @Override
    public LagerCharge speichern(LagerCharge charge) {
        return repository.save(charge);
    }

    @Override
    public void loeschen(LagerCharge charge) {
        repository.delete(charge);
    }
}
