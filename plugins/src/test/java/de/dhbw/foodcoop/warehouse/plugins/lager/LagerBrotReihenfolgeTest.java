package de.dhbw.foodcoop.warehouse.plugins.lager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import de.dhbw.foodcoop.warehouse.application.brot.BrotBestandService;
import de.dhbw.foodcoop.warehouse.application.lager.ProduktService;
import de.dhbw.foodcoop.warehouse.application.preishistorie.PreisHistorieService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;
import de.dhbw.foodcoop.warehouse.domain.repositories.BrotBestandRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.ProduktRepository;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;
import de.dhbw.foodcoop.warehouse.domain.values.Lagerbestand;

class LagerBrotReihenfolgeTest {

    @Test
    void lagerProdukteFolgenIhremPlatz() {
        List<Produkt> daten = new ArrayList<>(List.of(
                produkt("n", "Nudeln", 2),
                produkt("r", "Reis", 1),
                produkt("h", "Haferflocken", null)
        ));

        ProduktRepository repository = mock(ProduktRepository.class);
        when(repository.alle()).thenAnswer(i -> new ArrayList<>(daten));
        when(repository.findeMitId(anyString())).thenAnswer(i ->
                daten.stream().filter(p -> p.getId().equals(i.getArgument(0))).findFirst());
        when(repository.speichern(any())).thenAnswer(i -> i.getArgument(0));

        ProduktService service = new ProduktService(repository, mock(PreisHistorieService.class));

        assertThat(service.all()).extracting(Produkt::getName)
                .containsExactly("Reis", "Nudeln", "Haferflocken");

        service.reihenfolgeSetzen(List.of("h", "r"));

        assertThat(service.all()).extracting(Produkt::getName)
                .containsExactly("Haferflocken", "Reis", "Nudeln");
    }


    @Test
    void neuesBrotKommtAnDenGewuenschtenPlatz() {
        List<BrotBestand> daten = new ArrayList<>(List.of(
                brot("d", "Dinkelbrot", 1),
                brot("r", "Roggenbrot", 2)
        ));

        BrotBestandRepository repository = mock(BrotBestandRepository.class);
        when(repository.alle()).thenAnswer(i -> new ArrayList<>(daten));
        when(repository.findeMitId(anyString())).thenReturn(Optional.empty());
        when(repository.speichern(any())).thenAnswer(i -> {
            BrotBestand b = i.getArgument(0);
            if (!daten.contains(b)) {
                daten.add(b);
            }
            return b;
        });

        BrotBestandService service = new BrotBestandService(repository, mock(PreisHistorieService.class));

        service.save(brot("s", "Sonnenblumenbrot", 1));

        assertThat(service.all()).extracting(BrotBestand::getName)
                .containsExactly("Sonnenblumenbrot", "Dinkelbrot", "Roggenbrot");
    }


    private static Produkt produkt(String id, String name, Integer platz) {
        Produkt p = new Produkt(
                id, name, null,
                new Kategorie("k", "Trocken", false),
                new Lagerbestand(new Einheit("kg", "kg"), 1.0, 2.0),
                BigDecimal.ONE
        );
        p.setSortierung(platz);
        return p;
    }

    private static BrotBestand brot(String id, String name, Integer platz) {
        BrotBestand b = new BrotBestand(id, name, true, 1.0, BigDecimal.ONE);
        b.setSortierung(platz);
        return b;
    }
}
