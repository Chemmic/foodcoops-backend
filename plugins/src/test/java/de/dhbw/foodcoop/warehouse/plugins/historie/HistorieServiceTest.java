package de.dhbw.foodcoop.warehouse.plugins.historie;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.EinkaufEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.Kategorie;
import de.dhbw.foodcoop.warehouse.domain.repositories.BrotBestellungRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.EinkaufRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestellungRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.PreisHistorieRepository;
import de.dhbw.foodcoop.warehouse.domain.values.Einheit;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.BestellPosition;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Bestellrunde;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Historie;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Mitglied;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.RundenStatistik;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Statistik;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakAdminClient;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

class HistorieServiceTest {

    // Drei Runden: R1 (abgeschlossen), R2 (Einkauf), R3 (aktuell)
    private static final DeadlineEntity R1 = runde("r1", "2026-09-07T23:59");
    private static final DeadlineEntity R2 = runde("r2", "2026-09-14T23:59");
    private static final DeadlineEntity R3 = runde("r3", "2026-09-21T23:59");

    private static final Einheit KG = new Einheit("kg-id", "kg");
    private static final Kategorie GEMUESE = new Kategorie("k1", "Gemüse", false);

    private static final FrischBestand ZWIEBEL = new FrischBestand(
            "zwiebel", "Zwiebeln", true, "DE", 10f, KG, GEMUESE, new BigDecimal("2.00"), "Bio", false);

    private static final FrischBestand BLUMENKOHL = new FrischBestand(
            "blumenkohl", "Blumenkohl", true, "DE", 6f, KG, GEMUESE, new BigDecimal("4.00"), "Bio", true);

    private static final BrotBestand BROT = new BrotBestand(
            "brot", "Dinkelbrot", true, 1.0, new BigDecimal("5.00"));

    private EinkaufRepository einkaufe;
    private FrischBestellungRepository frisch;
    private BrotBestellungRepository brot;
    private PreisHistorieRepository preise;
    private KeycloakAdminClient keycloak;
    private HistorieService service;


    @BeforeEach
    void setUp() {
        einkaufe = mock(EinkaufRepository.class);
        frisch = mock(FrischBestellungRepository.class);
        brot = mock(BrotBestellungRepository.class);
        preise = mock(PreisHistorieRepository.class);
        keycloak = mock(KeycloakAdminClient.class);

        DeadlineRepository deadlines = mock(DeadlineRepository.class);
        when(deadlines.alle()).thenReturn(List.of(R3, R1, R2));

        when(preise.findePreiseVonDeadline(anyString())).thenReturn(Map.of());
        when(preise.findePreisverlaufVonBestand(anyString())).thenReturn(Map.of());

        service = new HistorieService(
                einkaufe, frisch, brot, deadlines, preise,
                mock(DeadlineService.class), keycloak
        );
    }


    @Test
    void groupsOrdersAndPurchasesIntoRounds() {
        FrischBestellung zwiebelnR2 = frischBestellung("b1", ZWIEBEL, 3, R2);
        FrischBestellung blumenkohlR2 = frischBestellung("b2", BLUMENKOHL, 2, R2);
        FrischBestellung zwiebelnR3 = frischBestellung("b3", ZWIEBEL, 1, R3);

        when(frisch.findeAlleVonPerson("anna"))
                .thenReturn(List.of(zwiebelnR2, blumenkohlR2, zwiebelnR3));
        when(brot.alleVonPerson("anna")).thenReturn(List.of());

        // Eingekauft während R3: 3,5 kg Zwiebeln (0,5 zu viel), Blumenkohl 1,2 kg
        EinkaufEntity einkauf = einkauf("e1", "anna", "2026-09-22T10:00",
                new BestellungBuyEntity("x1", zwiebelnR2, 3.5),
                new BestellungBuyEntity("x2", blumenkohlR2, 1.2));
        einkauf.setFreshPriceAtTime(11.8);
        einkauf.setDeliveryCostAtTime(1.18);

        when(einkaufe.alleVonPerson("anna")).thenReturn(List.of(einkauf));

        // Preis-Snapshot der Einkaufsrunde R3: Zwiebeln inzwischen 2,20 €
        when(preise.findePreiseVonDeadline("r3")).thenReturn(Map.of(
                ZWIEBEL.getId(), new BigDecimal("2.20")
        ));

        Historie historie = service.historie("anna");

        assertThat(historie.runden())
                .extracting(Bestellrunde::deadlineId, Bestellrunde::status)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("r3", "AKTUELL"),
                        org.assertj.core.groups.Tuple.tuple("r2", "EINKAUF")
                );

        Bestellrunde r2 = historie.runden().get(1);

        assertThat(r2.einkaeufe()).singleElement().satisfies(e -> {
            assertThat(e.gesamt()).isEqualTo(12.98);
            assertThat(e.positionen()).hasSize(2);
            assertThat(e.positionen().get(0).preis()).isEqualByComparingTo("2.20");
            assertThat(e.positionen().get(0).summe()).isEqualByComparingTo("7.70");
        });

        BestellPosition blumenkohl = position(r2, "Blumenkohl");
        assertThat(blumenkohl.spezialfall()).isTrue();
        assertThat(blumenkohl.genommen()).isEqualTo(1.2);
        assertThat(blumenkohl.differenz()).isNull();

        BestellPosition zwiebeln = position(r2, "Zwiebeln");
        assertThat(zwiebeln.differenz()).isEqualTo(0.5);
        assertThat(zwiebeln.status()).isEqualTo("GENOMMEN");

        assertThat(position(historie.runden().get(0), "Zwiebeln").status())
                .isEqualTo("OFFEN");
    }


    @Test
    void priceHistoryUsesLightweightQuery() {
        when(frisch.findeAlleVonPerson("anna"))
                .thenReturn(List.of(frischBestellung("b1", ZWIEBEL, 1, R3)));
        when(brot.alleVonPerson("anna")).thenReturn(List.of());
        when(einkaufe.alleVonPerson("anna")).thenReturn(List.of());

        java.util.LinkedHashMap<LocalDateTime, BigDecimal> verlauf = new java.util.LinkedHashMap<>();
        verlauf.put(R1.getDatum(), new BigDecimal("2.00"));
        verlauf.put(R2.getDatum(), new BigDecimal("2.10"));
        when(preise.findePreisverlaufVonBestand("zwiebel")).thenReturn(verlauf);

        assertThat(service.historie("anna").preisVerlauf()).singleElement()
                .satisfies(v -> {
                    assertThat(v.produkt()).isEqualTo("Zwiebeln");
                    assertThat(v.punkte()).extracting(p -> p.preis().toPlainString())
                            .containsExactly("2.00", "2.10");
                });
    }


    @Test
    void marksUncollectedOrdersInClosedRounds() {
        when(frisch.findeAlleVonPerson("anna")).thenReturn(List.of());
        when(brot.alleVonPerson("anna")).thenReturn(List.of(brotBestellung("b9", R1)));
        when(einkaufe.alleVonPerson("anna")).thenReturn(List.of());

        Bestellrunde r1 = service.historie("anna").runden().get(0);

        assertThat(r1.status()).isEqualTo("ABGESCHLOSSEN");
        assertThat(r1.bestellungen()).singleElement().satisfies(p -> {
            assertThat(p.typ()).isEqualTo("BROT");
            assertThat(p.einheit()).isEqualTo("Stück");
            assertThat(p.status()).isEqualTo("NICHT_ABGEHOLT");
        });
        assertThat(r1.geschaetzterBestellwert()).isEqualTo(10.0);
    }


    @Test
    void ordersWithoutDeadlineUseTheirDate() {
        FrischBestellung alt = frischBestellung("alt", ZWIEBEL, 1, null);
        alt.setDatum(LocalDateTime.parse("2026-09-10T12:00"));

        when(frisch.findeAlleVonPerson("anna")).thenReturn(List.of(alt));
        when(brot.alleVonPerson("anna")).thenReturn(List.of());
        when(einkaufe.alleVonPerson("anna")).thenReturn(List.of());

        assertThat(service.historie("anna").runden())
                .extracting(Bestellrunde::deadlineId)
                .containsExactly("r1");
    }


    @Test
    void statisticsSumSpendingPerRoundAndIncludeKeycloakMembers() {
        FrischBestellung b = frischBestellung("b1", ZWIEBEL, 3, R2);
        b.setPersonId("anna");

        when(frisch.alle()).thenReturn(List.of(b));
        when(brot.alle()).thenReturn(List.of());

        EinkaufEntity einkauf = einkauf("e1", "anna", "2026-09-22T10:00",
                new BestellungBuyEntity("x1", b, 3));
        einkauf.setFreshPriceAtTime(6.0);
        einkauf.setDeliveryCostAtTime(0.6);
        when(einkaufe.alle()).thenReturn(List.of(einkauf));

        when(keycloak.listUsers()).thenReturn(List.of(
                new User("u1", "anna", "anna@example.org", "Anna", "A", true, true, 1L, List.of(), null),
                new User("u2", "bert", null, null, null, true, true, 1L, List.of(), null)
        ));

        Statistik statistik = service.statistik(12);

        RundenStatistik r2 = statistik.runden().stream()
                .filter(r -> r.deadlineId().equals("r2"))
                .findFirst()
                .orElseThrow();

        assertThat(r2.gesamt()).isEqualTo(6.6);
        assertThat(r2.aktiveMitglieder()).isEqualTo(1);
        assertThat(statistik.topProdukte()).singleElement()
                .satisfies(p -> assertThat(p.bestellungen()).isEqualTo(1));
        assertThat(statistik.mitgliederGesamt()).isEqualTo(2);

        List<Mitglied> mitglieder = service.mitglieder();
        assertThat(mitglieder).extracting(Mitglied::personId).containsExactly("anna", "bert");
        assertThat(mitglieder.get(0).name()).isEqualTo("Anna A");
        assertThat(mitglieder.get(0).ausgegeben()).isEqualTo(6.6);
    }


    @Test
    void membersWorkWithoutKeycloak() {
        when(frisch.alle()).thenReturn(List.of());
        when(brot.alle()).thenReturn(List.of());
        when(einkaufe.alle()).thenReturn(List.of());
        when(keycloak.listUsers()).thenThrow(new RuntimeException("nicht erreichbar"));

        assertThat(service.mitglieder()).isEmpty();
    }


    // =========================================================================

    private static DeadlineEntity runde(String id, String datum) {
        return new DeadlineEntity(id, "Montag", Time.valueOf("23:59:00"), LocalDateTime.parse(datum));
    }

    private static FrischBestellung frischBestellung(
            String id, FrischBestand bestand, double menge, DeadlineEntity runde
    ) {
        FrischBestellung b = new FrischBestellung(
                id, "anna", bestand, menge,
                runde == null ? LocalDateTime.parse("2026-09-01T00:00") : runde.getDatum().plusHours(1),
                false
        );
        b.setDeadline(runde);
        return b;
    }

    private static BrotBestellung brotBestellung(String id, DeadlineEntity runde) {
        BrotBestellung b = new BrotBestellung(id, "anna", BROT, 2);
        b.setDatum(runde.getDatum().plusHours(1));
        b.setDeadline(runde);
        return b;
    }

    private static EinkaufEntity einkauf(
            String id, String person, String datum, BestellungBuyEntity... items
    ) {
        return new EinkaufEntity(
                id, person, List.of(items), List.of(), List.of(),
                LocalDateTime.parse(datum), 0, 0, 0, 0, 0
        );
    }

    private static BestellPosition position(Bestellrunde runde, String produkt) {
        return runde.bestellungen().stream()
                .filter(p -> p.produkt().equals(produkt))
                .findFirst()
                .orElseThrow();
    }
}
