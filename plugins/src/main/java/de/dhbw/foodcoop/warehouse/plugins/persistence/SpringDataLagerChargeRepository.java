package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;

public interface SpringDataLagerChargeRepository extends JpaRepository<LagerCharge, String> {

    List<LagerCharge> findByProduktIdOrderByEingelagertAmAscIdAsc(String produktId);

    List<LagerCharge> findAllByOrderByEingelagertAmAscIdAsc();
}
