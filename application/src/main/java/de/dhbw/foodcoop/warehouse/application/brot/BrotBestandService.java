package de.dhbw.foodcoop.warehouse.application.brot;

import de.dhbw.foodcoop.warehouse.application.reihenfolge.Reihenfolge;
import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.repositories.BrotBestandRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class BrotBestandService {
    private final BrotBestandRepository repository;
    private final PreisHistorieService preisHistorieService;

    @Autowired
    public BrotBestandService(BrotBestandRepository repository, PreisHistorieService preisHistorieService) {
        this.repository = repository;
        this.preisHistorieService = preisHistorieService;
    }

    public Optional<BrotBestand> findById(String id) {
        return repository.findeMitId(id);
    }

    /** Nach Platz in der Liste, dann Name. */
    public List<BrotBestand> all() {
        return Reihenfolge.sortiert(repository.alle());
    }

    public BrotBestand save(
            BrotBestand newBrotBestand
    ) {
        Optional<BrotBestand> vorhanden =
                newBrotBestand.getId() == null
                        ? Optional.empty()
                        : repository.findeMitId(newBrotBestand.getId());

        if (vorhanden.isEmpty()) {
            Reihenfolge.einordnen(all(), newBrotBestand, repository::speichern);
        } else if (newBrotBestand.getSortierung() == null) {
            newBrotBestand.setSortierung(vorhanden.get().getSortierung());
        }

        BrotBestand gespeichert =
                repository.speichern(
                        newBrotBestand
                );

        preisHistorieService
                .speichereAktuellenPreis(
                        gespeichert
                );

        return gespeichert;
    }

    
    public void deleteById(String id) {
    	Optional<BrotBestand> toBeDeleted = repository.findeMitId(id);
        if (toBeDeleted.isEmpty()) {
            return;
        }
        repository.deleteById(id);
    }
    
    public List<BrotBestand> allOrdered() {
    	return all();
    }


    /** Reihenfolge übernehmen (z.B. nach Drag & Drop): IDs von oben nach unten. */
    public List<BrotBestand> reihenfolgeSetzen(List<String> ids) {
        return Reihenfolge.setzen(all(), ids, repository::speichern);
    }


    /** Einmalig beim Start: Brote ohne Platz bekommen einen. */
    public int reihenfolgeInitialisieren() {
        return Reihenfolge.initialisieren(repository.alle(), repository::speichern);
    }
}
