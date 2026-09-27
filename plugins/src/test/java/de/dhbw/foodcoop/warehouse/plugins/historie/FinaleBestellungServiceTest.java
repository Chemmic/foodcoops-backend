package de.dhbw.foodcoop.warehouse.plugins.historie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.DiscrepancyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestellÜbersichtRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestandRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestellungRepository;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.FinaleBestellung;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.Position;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungService.FinaleBestellungException;

class FinaleBestellungServiceTest {

    // Deadlines D1..D4: die Übersicht an D4 enthält die Bestellungen der Runde D3 -> D4
    private static final DeadlineEntity D1 = deadline("d1", "2026-08-29T23:59");
    private static final DeadlineEntity D2 = deadline("d2", "2026-09-05T23:59");
    private static final DeadlineEntity D3 = deadline("d3", "2026-09-12T23:59");
    private static final DeadlineEntity D4 = deadline("d4", "2026-09-19T23:59");

    private static final Einheit STUECK = new Einheit("st", "Stück");
    private static final Kategorie SALAT = new Kategorie("salat", "Salat", true);
    private static final Kategorie GEMUESE = new Kategorie("gemuese", "Gemüse", false);

    private static final FrischBestand EICHBLATT = new FrischBestand(
            "eichblatt", "Eichblattsalat", true, "DE", 10f, STUECK, SALAT, new BigDecimal("1.50"), "Bio", false);

    private static final FrischBestand KOPFSALAT = new FrischBestand(
            "kopfsalat", "Kopfsalat", true, "DE", 10f, STUECK, SALAT, new BigDecimal("1.40"), "Bio", false);

    private static final FrischBestand LAUCH = new FrischBestand(
            "lauch", "Lauch", true, "DE", 5f, STUECK, GEMUESE, new BigDecimal("1.00"), "Bio", false);

    private static final BrotBestand BROT = new BrotBestand(
            "brot", "Dinkelbrot", true, 1.0, new BigDecimal("5.00"));

    private BestellÜbersichtRepository uebersichten;
    private DeadlineRepository deadlines;
    private FrischBestellungRepository frisch;
    private FrischBestandRepository bestaende;
    private FinaleBestellungService service;


    @BeforeEach
    void setUp() {
        uebersichten = mock(BestellÜbersichtRepository.class);
        deadlines = mock(DeadlineRepository.class);
        frisch = mock(FrischBestellungRepository.class);
        bestaende = mock(FrischBestandRepository.class);

        when(deadlines.alle()).thenReturn(List.of(D3, D1, D4, D2));
        when(deadlines.letzte()).thenReturn(Optional.of(D4));
        when(frisch.alle()).thenReturn(List.of());
        when(uebersichten.speichern(any())).thenAnswer(i -> i.getArgument(0));

        service = new FinaleBestellungService(
                uebersichten, deadlines, mock(DeadlineService.class), frisch, bestaende
        );
    }


    @Test
    void showsTheLastClosedRoundWithHistoryButNoPersons() {
        // Eichblatt wird seit 3 Runden gekauft, Kopfsalat seit 2 Runden gewünscht, nie gekauft
        when(uebersichten.findeMitDeadline(D2)).thenReturn(uebersicht(D2,
                new DiscrepancyEntity("a1", EICHBLATT, 1, 2f, 8f)));
        when(uebersichten.findeMitDeadline(D3)).thenReturn(uebersicht(D3,
                new DiscrepancyEntity("b1", EICHBLATT, 1, 1f, 9f),
                new DiscrepancyEntity("b2", KOPFSALAT, 0, -2f, 2f)));

        BestellUebersicht aktuell = uebersicht(D4,
                new DiscrepancyEntity("c1", EICHBLATT, 1, 2f, 8f),
                new DiscrepancyEntity("c2", KOPFSALAT, 0, -3f, 3f),
                new DiscrepancyEntity("c3", LAUCH, 1, 1f, 4f));
        BrotBestellung dinkel = new BrotBestellung("br", "anna", BROT, 2);
        aktuell.getBrotBestellung().add(dinkel);
        when(uebersichten.findeMitDeadline(D4)).thenReturn(aktuell);

        when(frisch.alle()).thenReturn(List.of(
                bestellung("o1", "anna", EICHBLATT, 5, D3),
                bestellung("o2", "bert", EICHBLATT, 3, D3),
                bestellung("o3", "carla", KOPFSALAT, 3, D3),
                // andere Runde zählt nicht
                bestellung("o4", "dora", KOPFSALAT, 1, D2)
        ));

        FinaleBestellung bestellung = service.laden(12);

        assertThat(bestellung.uebersichtId()).isEqualTo("u-d4");
        assertThat(bestellung.start()).isEqualTo(D3.getDatum());
        assertThat(bestellung.ende()).isEqualTo(D4.getDatum());
        assertThat(bestellung.verlaufRunden()).containsExactly(D3.getDatum(), D2.getDatum(), D1.getDatum());

        // Mischbare Kategorien zuerst
        assertThat(bestellung.kategorien())
                .extracting(FinaleBestellungDtos.Kategorie::name, FinaleBestellungDtos.Kategorie::mischbar)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Salat", true),
                        org.assertj.core.groups.Tuple.tuple("Gemüse", false)
                );

        Position eichblatt = position(bestellung, "eichblatt");
        assertThat(eichblatt.besteller()).isEqualTo(2);
        assertThat(eichblatt.zuVielZuWenig()).isEqualTo(2);
        assertThat(eichblatt.verlauf().gekauftInFolge()).isEqualTo(2);
        assertThat(eichblatt.verlauf().runden()).hasSize(3);
        assertThat(eichblatt.verlauf().runden().get(2)).isNull();

        Position kopfsalat = position(bestellung, "kopfsalat");
        assertThat(kopfsalat.besteller()).isEqualTo(1);
        assertThat(kopfsalat.verlauf().gekauftInFolge()).isZero();
        assertThat(kopfsalat.verlauf().gewuenschtInFolge()).isEqualTo(2);
        assertThat(kopfsalat.verlauf().davonOhneGebinde()).isEqualTo(1);

        assertThat(bestellung.brot()).singleElement().satisfies(b -> {
            assertThat(b.menge()).isEqualTo(2);
            assertThat(b.besteller()).isEqualTo(1);
        });
    }


    @Test
    void switchingTheMixedPackToAnotherVarietyOnlyChangesTheSummary() {
        BestellUebersicht aktuell = uebersicht(D4,
                new DiscrepancyEntity("c1", EICHBLATT, 1, 2f, 8f),
                new DiscrepancyEntity("c2", KOPFSALAT, 0, -2f, 2f));
        when(uebersichten.findeMitDeadline(D4)).thenReturn(aktuell);

        Position eichblatt = service.gebindeSetzen("eichblatt", 0.0);
        Position kopfsalat = service.gebindeSetzen("kopfsalat", 1.0);

        assertThat(eichblatt.zuBestellendeGebinde()).isZero();
        assertThat(eichblatt.zuVielZuWenig()).isEqualTo(-8);
        assertThat(kopfsalat.zuBestellendeGebinde()).isEqualTo(1);
        assertThat(kopfsalat.zuVielZuWenig()).isEqualTo(8);

        // Gewollte Mengen (= Bestellungen der Mitglieder) bleiben gleich
        assertThat(aktuell.getDiscrepancy())
                .extracting(DiscrepancyEntity::getGewollteMenge)
                .containsExactly(8f, 2f);
        verify(frisch, never()).speichern(any());
    }


    @Test
    void mergesDuplicateEntriesOfOneProduct() {
        BestellUebersicht aktuell = uebersicht(D4,
                new DiscrepancyEntity("c1", LAUCH, 1, 0f, 1f),
                new DiscrepancyEntity("c2", LAUCH, 2, 0f, 2f));
        when(uebersichten.findeMitDeadline(D4)).thenReturn(aktuell);

        Position lauch = service.gebindeSetzen("lauch", 1.0);

        assertThat(lauch.gewollteMenge()).isEqualTo(3);
        assertThat(lauch.zuBestellendeGebinde()).isEqualTo(1);
        assertThat(lauch.zuVielZuWenig()).isEqualTo(2);
    }


    @Test
    void addsAndRemovesExtraProducts() {
        BestellUebersicht aktuell = uebersicht(D4,
                new DiscrepancyEntity("c1", EICHBLATT, 1, 2f, 8f));
        when(uebersichten.findeMitDeadline(D4)).thenReturn(aktuell);
        when(bestaende.findeMitId("lauch")).thenReturn(Optional.of(LAUCH));

        Position lauch = service.hinzufuegen("lauch", 2.0);

        assertThat(lauch.gewollteMenge()).isZero();
        assertThat(lauch.zuVielZuWenig()).isEqualTo(10);
        assertThat(aktuell.getDiscrepancy()).hasSize(2);

        assertThatThrownBy(() -> service.hinzufuegen("lauch", 1.0))
                .isInstanceOf(FinaleBestellungException.class)
                .hasMessageContaining("schon in der Bestellung");

        // Von Mitgliedern bestellte Produkte bleiben drin
        assertThatThrownBy(() -> service.entfernen("eichblatt"))
                .isInstanceOf(FinaleBestellungException.class)
                .hasMessageContaining("Gebinde auf 0");

        service.entfernen("lauch");
        assertThat(aktuell.getDiscrepancy()).hasSize(1);
    }


    @Test
    void rejectsInvalidAmountsAndMissingSummary() {
        assertThatThrownBy(() -> service.gebindeSetzen("eichblatt", -1.0))
                .isInstanceOf(FinaleBestellungException.class);

        assertThatThrownBy(() -> service.gebindeSetzen("eichblatt", 1.0))
                .isInstanceOf(FinaleBestellungException.class)
                .hasMessageContaining("keine Bestellübersicht");

        when(deadlines.alle()).thenReturn(List.of());
        assertThat(service.laden(12).kategorien()).isEmpty();
    }


    // =========================================================================

    private static Position position(FinaleBestellung bestellung, String produktId) {
        return bestellung.kategorien().stream()
                .flatMap(k -> k.positionen().stream())
                .filter(p -> p.produktId().equals(produktId))
                .findFirst()
                .orElseThrow();
    }

    private static DeadlineEntity deadline(String id, String datum) {
        return new DeadlineEntity(id, "Samstag", Time.valueOf("23:59:00"), LocalDateTime.parse(datum));
    }

    private static FrischBestellung bestellung(
            String id, String person, FrischBestand bestand, double menge, DeadlineEntity runde
    ) {
        FrischBestellung b = new FrischBestellung(
                id, person, bestand, menge, runde.getDatum().plusHours(1), false
        );
        b.setDeadline(runde);
        return b;
    }

    private static BestellUebersicht uebersicht(DeadlineEntity deadline, DiscrepancyEntity... eintraege) {
        return new BestellUebersicht(
                new ArrayList<>(Arrays.asList(eintraege)),
                new ArrayList<>(),
                deadline,
                "u-" + deadline.getId()
        );
    }
}
