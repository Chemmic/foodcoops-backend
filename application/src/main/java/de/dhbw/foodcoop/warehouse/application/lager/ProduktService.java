package de.dhbw.foodcoop.warehouse.application.lager;

import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;
import de.dhbw.foodcoop.warehouse.domain.exceptions.ProduktInUseException;
import de.dhbw.foodcoop.warehouse.domain.repositories.ProduktRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProduktService {
    private final ProduktRepository repository;
    private final PreisHistorieService preisHistorieService;


    @Autowired
    public ProduktService(ProduktRepository repository, PreisHistorieService preisHistorieService) {
        this.repository = repository;
        this.preisHistorieService = preisHistorieService;
    }

    public Optional<Produkt> findById(String id) {
        return repository.findeMitId(id);
    }

    public List<Produkt> all() {
        return repository.alle();
    }

    public Produkt save(
            Produkt newProdukt
    ) {
        Produkt gespeichert =
                repository.speichern(
                        newProdukt
                );

        preisHistorieService
                .speichereAktuellenPreis(
                        gespeichert
                );

        return gespeichert;
    }

    public void deleteById(String id) throws ProduktInUseException {
        Optional<Produkt> toBeDeleted = repository.findeMitId(id);
        if(toBeDeleted.isEmpty()) {
            return;
        }
        if(toBeDeleted.get().getLagerbestand().getIstLagerbestand() > 1.E-3) {
            throw new ProduktInUseException(id);
        }
        repository.deleteById(id);
    }
}
