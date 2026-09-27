package de.dhbw.foodcoop.warehouse.application.frischbestellung;

import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;
import de.dhbw.foodcoop.warehouse.application.reihenfolge.Reihenfolge;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestandRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FrischBestandService {
    private final FrischBestandRepository repository;
    private final PreisHistorieService preisHistorieService;

    @Autowired
    public FrischBestandService(FrischBestandRepository repository, PreisHistorieService preisHistorieService) {
        this.repository = repository;
        this.preisHistorieService = preisHistorieService;
    }

    public Optional<FrischBestand> findById(String id) {
        return repository.findeMitId(id);
    }

    /** Nach Platz in der Liste (wie beim Händler), dann Name. */
    public List<FrischBestand> all() {
        return Reihenfolge.sortiert(repository.alle());
    }

    public FrischBestand save(FrischBestand newFrischBestand) {
        Optional<FrischBestand> vorhanden =
                newFrischBestand.getId() == null
                        ? Optional.empty()
                        : repository.findeMitId(newFrischBestand.getId());

        if (vorhanden.isEmpty()) {
            Reihenfolge.einordnen(all(), newFrischBestand, repository::speichern);
        } else if (newFrischBestand.getSortierung() == null) {
            newFrischBestand.setSortierung(vorhanden.get().getSortierung());
        }

        FrischBestand gespeichert = repository.speichern(newFrischBestand);
        preisHistorieService.speichereAktuellenPreis(gespeichert);
        return gespeichert;
    }


    public void deleteById(String id) {Optional<FrischBestand> toBeDeleted = repository.findeMitId(id);
        if (toBeDeleted.isEmpty()) {
            return;
        }
        // if (frischBestellungService.all().stream()
        //         .anyMatch(frischbestellung -> frischbestellung.getFrischbestand().equals(toBeDeleted.get()))) {
        //     throw new FrischBestandInUseException(id);
        // }
        repository.deleteById(id);
    }

    public List<FrischBestand> allOrdered() {
    	return all();
    }


    /** Reihenfolge übernehmen (z.B. nach Drag & Drop): IDs von oben nach unten. */
    public List<FrischBestand> reihenfolgeSetzen(List<String> ids) {
        return Reihenfolge.setzen(all(), ids, repository::speichern);
    }


    /** Einmalig beim Start: Produkte ohne Platz bekommen einen. */
    public int reihenfolgeInitialisieren() {
        return Reihenfolge.initialisieren(repository.alle(), repository::speichern);
    }
}
