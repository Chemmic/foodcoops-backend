package de.dhbw.foodcoop.warehouse.application.brot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.repositories.BrotBestellungRepository;

@Service
public class BrotBestellungService {
    private final BrotBestellungRepository repository;
    private final DeadlineService deadlineService;
    @Autowired
    public BrotBestellungService(BrotBestellungRepository repository, DeadlineService deadlineService) {
        this.repository = repository;
        this.deadlineService = deadlineService;
    }

    public List<BrotBestellung> all() {
        return repository.alle();
    }
    
    public List<BrotBestellung> findAllAftetDate(LocalDateTime date) {
    	return repository.findAllOrdersAfterDate(date);
    }

    public List<BrotBestellung> findByDateAfterAndPerson(LocalDateTime datum, String person_id){
        return repository.findeMitDatumNachUndPerson(datum, person_id);
    }

    public List<BrotBestellung> findByDateAfterAndSum(LocalDateTime datum){
        return repository.findeMitDatumNachUndSum(datum);
    }

    public List<BrotBestellung> findByDateBetween(LocalDateTime datum1, LocalDateTime datum2, String person_id){
        return repository.findeMitDatumZwischen(datum1, datum2, person_id);
    }
    
    public List<BrotBestellung> findByDateBetween(LocalDateTime datum1, LocalDateTime datum2){
        return repository.findeMitDatumZwischen(datum1, datum2);
    }

    public BrotBestellung save(
            BrotBestellung bestellung
    ) {

        deadlineService.updateDeadline();

        if (bestellung.getDatum() == null) {
            bestellung.setDatum(
                    LocalDateTime.now()
            );
        }

        if (bestellung.getDeadline() == null) {
            bestellung.setDeadline(
                    deadlineService.last()
            );
        }

        return repository.speichern(
                bestellung
        );
    }

    public Optional<BrotBestellung> findById(String id) {
        return repository.findeMitId(id);
    }

    public void deleteById(String id) {
        repository.deleteById(id);
    }

    public List<BrotBestellung> alleVonPersonUndDeadline(
            String personId,
            String deadlineId
    ) {
        return repository
                .findeVonPersonUndDeadline(
                        personId,
                        deadlineId
                );
    }

    public List<BrotBestellung> findAllByPerson(
            String personId) {

        return repository.alleVonPerson(
                personId
        );
    }

    public List<BrotBestellung> findByDeadlineAndPerson(
            String deadlineId,
            String personId) {

        return repository.findeVonPersonUndDeadline(
                personId,
                deadlineId
        );
    }

    public List<BrotBestellung> findAllByDeadline(
            String deadlineId) {

        return repository.findeAlleVonDeadline(
                deadlineId
        );
    }
}   
