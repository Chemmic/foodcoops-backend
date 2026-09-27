package de.dhbw.foodcoop.warehouse.plugins.lager;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dhbw.foodcoop.warehouse.application.lager.LagerChargenService;
import de.dhbw.foodcoop.warehouse.application.lager.LagerChargenService.Entnahme;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.entities.LagerCharge;
import de.dhbw.foodcoop.warehouse.domain.entities.Produkt;
import de.dhbw.foodcoop.warehouse.domain.repositories.LagerChargeRepository;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;
import de.dhbw.foodcoop.warehouse.domain.values.Lagerbestand;

class LagerChargenServiceTest {

    private static final LocalDateTime ALT = LocalDateTime.of(2026, 8, 1, 10, 0);

    private FakeRepository repository;
    private LagerChargenService service;


    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        service = new LagerChargenService(repository);
    }


    @Test
    void oldStockIsSoldFirstAtOldPrice() {
        // 2 kg zu 3,00 €, dann neue Lieferung 5 kg zu 3,50 €
        repository.speichern(new LagerCharge("reis", 2, new BigDecimal("3.00"), ALT));
        repository.speichern(new LagerCharge("reis", 5, new BigDecimal("3.50"), ALT.plusDays(7)));

        Entnahme entnahme = service.entnehmen("reis", 7, new BigDecimal("3.50"), 3);

        // 2 kg * 3,00 + 1 kg * 3,50
        assertThat(entnahme.betrag()).isEqualByComparingTo("9.50");
        assertThat(entnahme.teile()).hasSize(2);

        // Alte Charge ist aufgebraucht, von der neuen sind 4 kg übrig
        assertThat(repository.vonProdukt("reis"))
                .extracting(LagerCharge::getMenge, c -> c.getPreis().toPlainString())
                .containsExactly(org.assertj.core.groups.Tuple.tuple(4.0, "3.50"));
    }


    @Test
    void restockKeepsPriceOfExistingStock() {
        repository.speichern(new LagerCharge("reis", 2, new BigDecimal("3.00"), ALT));

        // Ist 2 -> 7, Preis 3,00 -> 3,50
        service.bestandGeaendert("reis", 2, new BigDecimal("3.00"), 7, new BigDecimal("3.50"), false);

        assertThat(repository.vonProdukt("reis"))
                .extracting(LagerCharge::getMenge, c -> c.getPreis().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2.0, "3.00"),
                        org.assertj.core.groups.Tuple.tuple(5.0, "3.50")
                );
    }


    @Test
    void restockAtSamePriceIsAddedUp() {
        repository.speichern(new LagerCharge("reis", 2, new BigDecimal("3.00"), ALT));
        repository.speichern(new LagerCharge("reis", 1, new BigDecimal("3.50"), ALT.plusDays(1)));

        // Gleicher Preis wie die jüngste Lieferung -> dazuzählen
        service.bestandGeaendert("reis", 3, new BigDecimal("3.50"), 5, new BigDecimal("3.50"), false);

        assertThat(repository.vonProdukt("reis"))
                .extracting(LagerCharge::getMenge, c -> c.getPreis().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2.0, "3.00"),
                        org.assertj.core.groups.Tuple.tuple(3.0, "3.50")
                );
    }


    @Test
    void existingBatchesWithSamePriceAreMerged() {
        // So entstanden vor der Änderung: mehrere Chargen zum gleichen Preis
        repository.speichern(new LagerCharge("polenta", 2, new BigDecimal("2.50"), ALT));
        repository.speichern(new LagerCharge("polenta", 2, new BigDecimal("2.51"), ALT.plusDays(1)));
        repository.speichern(new LagerCharge("polenta", 1, new BigDecimal("2.51"), ALT.plusDays(2)));

        // Anzeige fasst zusammen, ohne zu speichern
        assertThat(service.chargen(produkt("polenta", 5, "2.51")))
                .extracting(LagerCharge::getMenge)
                .containsExactly(2.0, 3.0);
        assertThat(repository.vonProdukt("polenta")).hasSize(3);

        // Beim nächsten Ändern wird es auch gespeichert
        service.bestandGeaendert("polenta", 5, new BigDecimal("2.51"), 6, new BigDecimal("2.51"), false);

        assertThat(repository.vonProdukt("polenta"))
                .extracting(LagerCharge::getMenge, c -> c.getPreis().toPlainString())
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple(2.0, "2.50"),
                        org.assertj.core.groups.Tuple.tuple(4.0, "2.51")
                );
    }


    @Test
    void stockMayExceedTarget() {
        // Soll dient nur der Einkaufsliste – mehr Ware als Soll ist erlaubt
        Lagerbestand bestand = new Lagerbestand(new Einheit("kg", "kg"), 20.0, 10.0);

        assertThat(bestand.nachbestellen()).isFalse();
        assertThat(new Lagerbestand(new Einheit("kg", "kg"), 9.0, 10.0).differenz()).isEqualTo(1.0);
    }


    @Test
    void newPriceCanBeAppliedToExistingStock() {
        repository.speichern(new LagerCharge("reis", 2, new BigDecimal("3.00"), ALT));
        repository.speichern(new LagerCharge("reis", 1, new BigDecimal("3.20"), ALT.plusDays(1)));

        service.bestandGeaendert("reis", 3, new BigDecimal("3.20"), 3, new BigDecimal("3.50"), true);

        assertThat(repository.vonProdukt("reis"))
                .allSatisfy(c -> assertThat(c.getPreis()).isEqualByComparingTo("3.50"));
    }


    @Test
    void lowerStockReducesOldestFirst() {
        repository.speichern(new LagerCharge("reis", 2, new BigDecimal("3.00"), ALT));
        repository.speichern(new LagerCharge("reis", 5, new BigDecimal("3.50"), ALT.plusDays(7)));

        // Inventur: nur noch 4 kg da
        service.bestandGeaendert("reis", 7, new BigDecimal("3.50"), 4, new BigDecimal("3.50"), false);

        assertThat(repository.vonProdukt("reis"))
                .extracting(LagerCharge::getMenge)
                .containsExactly(4.0);
    }


    @Test
    void stockWithoutBatchesBecomesOldestBatch() {
        // 3 kg Altbestand ohne Charge, 1 kg neu zu 4,00 €
        repository.speichern(new LagerCharge("nuss", 1, new BigDecimal("4.00"), ALT));

        Entnahme entnahme = service.entnehmen("nuss", 4, new BigDecimal("4.00"), 2);

        assertThat(entnahme.betrag()).isEqualByComparingTo("8.00");
        assertThat(repository.vonProdukt("nuss"))
                .extracting(LagerCharge::getMenge)
                .containsExactly(2.0); // gleicher Preis -> eine Charge
    }


    @Test
    void readingDoesNotChangeStoredBatches() {
        repository.speichern(new LagerCharge("reis", 5, new BigDecimal("3.00"), ALT));

        List<LagerCharge> gelesen = service.chargen(produkt("reis", 3, "3.20"));

        assertThat(gelesen).extracting(LagerCharge::getMenge).containsExactly(3.0);
        assertThat(repository.vonProdukt("reis").get(0).getMenge()).isEqualTo(5.0);
    }


    @Test
    void priceWhenStockIsNotEnoughUsesFallback() {
        List<LagerCharge> chargen = List.of(new LagerCharge("reis", 1, new BigDecimal("3.00"), ALT));

        Entnahme entnahme = LagerChargenService.preisFuer(chargen, 1.5, new BigDecimal("4.00"));

        assertThat(entnahme.betrag()).isEqualByComparingTo("5.00");
    }


    @Test
    void migrationCreatesOneBatchPerStockedProduct() {
        repository.speichern(new LagerCharge("schon-da", 1, BigDecimal.ONE, ALT));

        int angelegt = service.migrieren(List.of(
                produkt("reis", 3, "3.20"),
                produkt("leer", 0, "1.00"),
                produkt("schon-da", 1, "1.00")
        ));

        assertThat(angelegt).isEqualTo(1);
        assertThat(repository.vonProdukt("reis"))
                .extracting(LagerCharge::getMenge)
                .containsExactly(3.0);

        // Beim zweiten Start passiert nichts mehr
        assertThat(service.migrieren(List.of(produkt("reis", 3, "3.20")))).isZero();
    }


    // =========================================================================

    private static Produkt produkt(String id, double ist, String preis) {
        return new Produkt(
                id, id, null,
                new Kategorie("k", "Trocken", false),
                new Lagerbestand(new Einheit("kg", "kg"), ist, 10.0),
                new BigDecimal(preis)
        );
    }


    /** Einfaches In-Memory-Repository, sortiert wie die Datenbank-Abfrage. */
    private static final class FakeRepository implements LagerChargeRepository {

        private final List<LagerCharge> daten = new ArrayList<>();

        @Override
        public List<LagerCharge> vonProdukt(String produktId) {
            return alle().stream().filter(c -> c.getProduktId().equals(produktId)).toList();
        }

        @Override
        public List<LagerCharge> alle() {
            return daten.stream()
                    .sorted(Comparator.comparing(LagerCharge::getEingelagertAm).thenComparing(LagerCharge::getId))
                    .toList();
        }

        @Override
        public LagerCharge speichern(LagerCharge charge) {
            daten.removeIf(c -> c.getId().equals(charge.getId()));
            daten.add(charge);
            return charge;
        }

        @Override
        public void loeschen(LagerCharge charge) {
            daten.removeIf(c -> c.getId().equals(charge.getId()));
        }
    }
}
