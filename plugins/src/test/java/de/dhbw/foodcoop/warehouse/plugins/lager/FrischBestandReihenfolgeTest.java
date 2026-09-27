package de.dhbw.foodcoop.warehouse.plugins.lager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestandService;
import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestandRepository;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;

class FrischBestandReihenfolgeTest {

    private FakeRepository repository;
    private FrischBestandService service;


    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        service = new FrischBestandService(repository, mock(PreisHistorieService.class));
    }


    @Test
    void existingOrderIsKeptOnFirstStart() {
        // Reihenfolge wie bisher in der Datenbank, nicht alphabetisch
        repository.daten.add(produkt("z", "Zwiebeln", null));
        repository.daten.add(produkt("a", "Äpfel", null));
        repository.daten.add(produkt("k", "Karotten", null));

        assertThat(service.reihenfolgeInitialisieren()).isEqualTo(3);
        assertThat(namen()).containsExactly("Zwiebeln", "Äpfel", "Karotten");

        assertThat(service.reihenfolgeInitialisieren()).isZero();
    }


    @Test
    void newProductGoesToRequestedPlace() {
        repository.daten.add(produkt("z", "Zwiebeln", 1));
        repository.daten.add(produkt("a", "Äpfel", 2));
        repository.daten.add(produkt("k", "Karotten", 3));

        service.save(produkt("l", "Lauch", 2));

        assertThat(namen()).containsExactly("Zwiebeln", "Lauch", "Äpfel", "Karotten");
        assertThat(service.all()).extracting(FrischBestand::getSortierung).containsExactly(1, 2, 3, 4);
    }


    @Test
    void newProductWithoutPlaceGoesToTheEnd() {
        repository.daten.add(produkt("z", "Zwiebeln", 1));
        repository.daten.add(produkt("a", "Äpfel", 2));

        service.save(produkt("l", "Lauch", null));
        service.save(produkt("m", "Mangold", 99));

        assertThat(namen()).containsExactly("Zwiebeln", "Äpfel", "Lauch", "Mangold");
    }


    @Test
    void editingKeepsThePlace() {
        repository.daten.add(produkt("z", "Zwiebeln", 1));
        repository.daten.add(produkt("a", "Äpfel", 2));

        // Mapper setzt beim Ändern den alten Platz; ohne Angabe bleibt er auch
        FrischBestand geaendert = produkt("z", "Rote Zwiebeln", null);
        service.save(geaendert);

        assertThat(namen()).containsExactly("Rote Zwiebeln", "Äpfel");
    }


    @Test
    void dragAndDropOrderIsSaved() {
        repository.daten.add(produkt("z", "Zwiebeln", 1));
        repository.daten.add(produkt("a", "Äpfel", 2));
        repository.daten.add(produkt("k", "Karotten", 3));

        // Unbekannte IDs werden ignoriert, fehlende hinten angehängt
        service.reihenfolgeSetzen(List.of("k", "gibt-es-nicht", "z"));

        assertThat(namen()).containsExactly("Karotten", "Zwiebeln", "Äpfel");
    }


    // =========================================================================

    private List<String> namen() {
        return service.all().stream().map(FrischBestand::getName).toList();
    }

    private static FrischBestand produkt(String id, String name, Integer platz) {
        FrischBestand f = new FrischBestand(
                id, name, true, "DE", 5f,
                new Einheit("kg", "kg"),
                new Kategorie("g", "Gemüse", false),
                BigDecimal.ONE, "Bio", false
        );
        f.setSortierung(platz);
        return f;
    }


    private static final class FakeRepository implements FrischBestandRepository {

        final List<FrischBestand> daten = new ArrayList<>();

        @Override
        public List<FrischBestand> alle() {
            return new ArrayList<>(daten);
        }

        @Override
        public FrischBestand speichern(FrischBestand f) {
            for (int i = 0; i < daten.size(); i++) {
                if (daten.get(i).getId().equals(f.getId())) {
                    daten.set(i, f);
                    return f;
                }
            }
            daten.add(f);
            return f;
        }

        @Override
        public Optional<FrischBestand> findeMitId(String id) {
            return daten.stream().filter(f -> f.getId().equals(id)).findFirst();
        }

        @Override
        public void deleteById(String id) {
            daten.removeIf(f -> f.getId().equals(id));
        }

        @Override
        public List<FrischBestand> alleSortiert() {
            return alle();
        }
    }
}
