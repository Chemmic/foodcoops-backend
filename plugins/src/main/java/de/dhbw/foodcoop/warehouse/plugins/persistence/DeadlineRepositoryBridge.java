package de.dhbw.foodcoop.warehouse.plugins.persistence;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;

@Repository
public class DeadlineRepositoryBridge implements DeadlineRepository{
    private final SpringDataDeadlineRepository springDataDeadlineRepository;

    @Autowired
    public DeadlineRepositoryBridge(SpringDataDeadlineRepository springDataDeadlineRepository) {
        this.springDataDeadlineRepository = springDataDeadlineRepository;
    }

    @Override
    public List<DeadlineEntity> alle() {
        return springDataDeadlineRepository.findAll();
    }

    @Override
    public Optional<DeadlineEntity> letzte() {
        return springDataDeadlineRepository.findLast();
    }

    @Override
    public DeadlineEntity speichern(DeadlineEntity deadline) {
        return springDataDeadlineRepository.save(deadline);
    }

    @Override
    public Optional<DeadlineEntity> findeMitId(String id) {
        return springDataDeadlineRepository.findById(id);
    }

    @Override
    public void deleteById(String id) {
        springDataDeadlineRepository.deleteById(id);
    }

	@Override
	public Optional<DeadlineEntity> findeNachReihenfolge(int position) {
		// TODO Auto-generated method stub
		return springDataDeadlineRepository.findFromSortedPosition(position);
	}
}
