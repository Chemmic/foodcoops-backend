package de.dhbw.foodcoop.warehouse.plugins.historie;

import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.bestandVon;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.einheitVon;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.istSpezialfall;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.kategorieVon;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.liste;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.produktName;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.runden2;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.runden3;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.EinkaufEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.TooMuchBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.BrotBestellungRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.EinkaufRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestellungRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.PreisHistorieRepository;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.BestellPosition;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Bestellrunde;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Einkauf;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.EinkaufPosition;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Historie;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Mitglied;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.PreisPunkt;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.PreisVerlauf;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.ProduktStatistik;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.RundenStatistik;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Statistik;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakAdminClient;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

/**
 * ============================================================================
 * Bestellhistorie & Statistik
 * ============================================================================
 *
 * Zuordnung:
 *
 *   Bestellung -> Runde ihrer Deadline (ältere Bestellungen ohne Deadline:
 *                 Runde, in der sie aufgegeben wurden)
 *
 *   Einkauf    -> Runde der Bestellungen, die er abholt. Enthält er nur
 *                 Lagerware / "Zu viel": die Runde vor der, in der
 *                 eingekauft wurde (das ist die einkaufbare Runde).
 *
 * Preise pro Position stammen aus dem Preis-Snapshot der Runde, in der
 * eingekauft wurde (Fallback: aktueller Preis). Die Summen eines Einkaufs
 * sind die beim Einkauf gespeicherten Werte.
 *
 * ============================================================================
 */
@Service
@Transactional(readOnly = true)
public class HistorieService {

    private static final int PREISVERLAUF_PRODUKTE = 5;
    private static final int PREISVERLAUF_PUNKTE = 26;


    private final EinkaufRepository einkaufRepository;
    private final FrischBestellungRepository frischRepository;
    private final BrotBestellungRepository brotRepository;
    private final DeadlineRepository deadlineRepository;
    private final PreisHistorieRepository preisRepository;
    private final DeadlineService deadlineService;
    private final KeycloakAdminClient keycloak;


    public HistorieService(
            EinkaufRepository einkaufRepository,
            FrischBestellungRepository frischRepository,
            BrotBestellungRepository brotRepository,
            DeadlineRepository deadlineRepository,
            PreisHistorieRepository preisRepository,
            DeadlineService deadlineService,
            KeycloakAdminClient keycloak
    ) {
        this.einkaufRepository = einkaufRepository;
        this.frischRepository = frischRepository;
        this.brotRepository = brotRepository;
        this.deadlineRepository = deadlineRepository;
        this.preisRepository = preisRepository;
        this.deadlineService = deadlineService;
        this.keycloak = keycloak;
    }


    // =========================================================================
    // Persönliche Historie
    // =========================================================================

    public Historie historie(String personId) {
        Zeitachse zeit = zeitachse();
        Preise preise = new Preise();

        List<BestellungEntity> bestellungen = new ArrayList<>();
        bestellungen.addAll(frischRepository.findeAlleVonPerson(personId));
        bestellungen.addAll(brotRepository.alleVonPerson(personId));

        List<EinkaufEntity> einkaeufe =
                new ArrayList<>(einkaufRepository.alleVonPerson(personId));

        // Genommene Mengen je Bestellung (über alle Einkäufe)
        Map<String, Double> genommen = new HashMap<>();

        for (EinkaufEntity einkauf : einkaeufe) {
            for (BestellungBuyEntity item : liste(einkauf.getBestellungsEinkauf())) {
                if (item.getBestellung() != null) {
                    genommen.merge(item.getBestellung().getId(), item.getAmount(), Double::sum);
                }
            }
        }

        Map<String, List<BestellungEntity>> bestellungenJeRunde =
                bestellungen.stream()
                        .filter(b -> zeit.rundeVon(b) != null)
                        .collect(Collectors.groupingBy(b -> zeit.rundeVon(b).getId()));

        Map<String, List<EinkaufEntity>> einkaeufeJeRunde =
                einkaeufe.stream()
                        .filter(e -> zeit.rundeVon(e) != null)
                        .collect(Collectors.groupingBy(e -> zeit.rundeVon(e).getId()));

        Set<String> rundenIds = new HashSet<>();
        rundenIds.addAll(bestellungenJeRunde.keySet());
        rundenIds.addAll(einkaeufeJeRunde.keySet());

        List<Bestellrunde> runden =
                zeit.absteigend()
                        .stream()
                        .filter(d -> rundenIds.contains(d.getId()))
                        .map(d -> runde(
                                d,
                                zeit,
                                preise,
                                bestellungenJeRunde.getOrDefault(d.getId(), List.of()),
                                einkaeufeJeRunde.getOrDefault(d.getId(), List.of()),
                                genommen
                        ))
                        .toList();

        return new Historie(
                personId,
                runden,
                preisVerlauf(bestellungen, einkaeufe)
        );
    }


    private Bestellrunde runde(
            DeadlineEntity deadline,
            Zeitachse zeit,
            Preise preise,
            List<BestellungEntity> bestellungen,
            List<EinkaufEntity> einkaeufe,
            Map<String, Double> genommen
    ) {
        String status = zeit.status(deadline);

        List<BestellPosition> positionen =
                bestellungen.stream()
                        .sorted(Comparator
                                .comparing((BestellungEntity b) -> b instanceof BrotBestellung)
                                .thenComparing(b -> produktName(bestandVon(b)), String.CASE_INSENSITIVE_ORDER))
                        .map(b -> position(b, deadline, status, preise, genommen))
                        .toList();

        List<Einkauf> einkaufListe =
                einkaeufe.stream()
                        .sorted(Comparator.comparing(EinkaufEntity::getDate, Comparator.nullsLast(Comparator.naturalOrder())))
                        .map(e -> einkauf(e, zeit, preise))
                        .toList();

        double geschaetzt =
                positionen.stream()
                        .filter(p -> p.preis() != null && !p.spezialfall())
                        .mapToDouble(p -> p.preis().doubleValue() * p.bestellt())
                        .sum();

        double ausgegeben =
                einkaufListe.stream()
                        .mapToDouble(Einkauf::gesamt)
                        .sum();

        return new Bestellrunde(
                deadline.getId(),
                deadline.getDatum(),
                zeit.ende(deadline),
                status,
                positionen,
                einkaufListe,
                runden2(geschaetzt),
                runden2(ausgegeben)
        );
    }


    private BestellPosition position(
            BestellungEntity bestellung,
            DeadlineEntity runde,
            String rundenStatus,
            Preise preise,
            Map<String, Double> genommen
    ) {
        BestandEntity bestand = bestandVon(bestellung);
        boolean spezialfall = istSpezialfall(bestand);

        Double menge = genommen.get(bestellung.getId());

        String status =
                menge != null
                        ? "GENOMMEN"
                        : "ABGESCHLOSSEN".equals(rundenStatus)
                                ? "NICHT_ABGEHOLT"
                                : "OFFEN";

        Double differenz =
                menge != null && !spezialfall
                        ? runden3(menge - bestellung.getBestellmenge())
                        : null;

        return new BestellPosition(
                bestellung.getId(),
                bestellung instanceof BrotBestellung ? "BROT" : "FRISCH",
                bestand == null ? null : bestand.getId(),
                produktName(bestand),
                einheitVon(bestand),
                kategorieVon(bestand),
                bestellung.getBestellmenge(),
                menge,
                differenz,
                spezialfall,
                preise.preis(bestand, runde),
                status
        );
    }


    private Einkauf einkauf(
            EinkaufEntity einkauf,
            Zeitachse zeit,
            Preise preise
    ) {
        // Preise der Runde, in der eingekauft wurde
        DeadlineEntity kaufRunde = zeit.rundeZu(einkauf.getDate());

        List<EinkaufPosition> positionen = new ArrayList<>();

        for (BestellungBuyEntity item : liste(einkauf.getBestellungsEinkauf())) {
            BestellungEntity bestellung = item.getBestellung();
            BestandEntity bestand = bestellung == null ? null : bestandVon(bestellung);

            positionen.add(position(
                    bestellung instanceof BrotBestellung ? "BROT" : "FRISCH",
                    bestand,
                    bestellung == null ? null : bestellung.getBestellmenge(),
                    item.getAmount(),
                    preise.preis(bestand, kaufRunde)
            ));
        }

        for (BestandBuyEntity item : liste(einkauf.getBestandEinkauf())) {
            // Bezahlter Betrag, falls bekannt (ältere Lieferungen zum alten Preis)
            BigDecimal preis =
                    item.getBetrag() != null && item.getAmount() > 0
                            ? item.getBetrag().divide(BigDecimal.valueOf(item.getAmount()), 4, RoundingMode.HALF_UP)
                            : preise.preis(item.getBestand(), kaufRunde);

            positionen.add(position(
                    "LAGER",
                    item.getBestand(),
                    null,
                    item.getAmount(),
                    preis
            ));
        }

        for (TooMuchBuyEntity item : liste(einkauf.getTooMuchEinkauf())) {
            BestandEntity bestand =
                    item.getDiscrepancy() == null ? null : item.getDiscrepancy().getBestand();

            positionen.add(position(
                    "ZU_VIEL",
                    bestand,
                    null,
                    item.getAmount(),
                    preise.preis(bestand, kaufRunde)
            ));
        }

        double lieferkosten = runden2(einkauf.getDeliveryCostAtTime());

        return new Einkauf(
                einkauf.getId(),
                einkauf.getDate(),
                positionen,
                runden2(einkauf.getFreshPriceAtTime()),
                runden2(einkauf.getBreadPriceAtTime()),
                runden2(einkauf.getBestandPriceAtTime()),
                runden2(einkauf.getTooMuchPriceAtTime()),
                lieferkosten,
                runden2(einkauf.getTotalPriceAtTime() + lieferkosten)
        );
    }


    private static EinkaufPosition position(
            String typ,
            BestandEntity bestand,
            Double bestellt,
            double menge,
            BigDecimal preis
    ) {
        return new EinkaufPosition(
                typ,
                produktName(bestand),
                einheitVon(bestand),
                bestellt,
                menge,
                preis,
                preis == null
                        ? null
                        : preis.multiply(BigDecimal.valueOf(menge)).setScale(2, RoundingMode.HALF_UP),
                istSpezialfall(bestand)
        );
    }


    /**
     * Preisentwicklung der Produkte, die die Person am häufigsten kauft.
     */
    private List<PreisVerlauf> preisVerlauf(
            List<BestellungEntity> bestellungen,
            List<EinkaufEntity> einkaeufe
    ) {
        Map<String, BestandEntity> bestaende = new HashMap<>();
        Map<String, Integer> haeufigkeit = new HashMap<>();

        Stream.concat(
                bestellungen.stream().map(HistorieHilfen::bestandVon),
                einkaeufe.stream()
                        .flatMap(e -> liste(e.getBestandEinkauf()).stream())
                        .map(BestandBuyEntity::getBestand)
        )
                .filter(Objects::nonNull)
                .forEach(b -> {
                    bestaende.putIfAbsent(b.getId(), b);
                    haeufigkeit.merge(b.getId(), 1, Integer::sum);
                });

        return haeufigkeit.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(PREISVERLAUF_PRODUKTE)
                .map(eintrag -> {
                    BestandEntity bestand = bestaende.get(eintrag.getKey());

                    List<PreisPunkt> punkte =
                            preisRepository.findePreisverlaufVonBestand(bestand.getId())
                                    .entrySet()
                                    .stream()
                                    .map(p -> new PreisPunkt(p.getKey(), p.getValue()))
                                    .toList();

                    if (punkte.size() > PREISVERLAUF_PUNKTE) {
                        punkte = punkte.subList(punkte.size() - PREISVERLAUF_PUNKTE, punkte.size());
                    }

                    return new PreisVerlauf(
                            bestand.getId(),
                            produktName(bestand),
                            einheitVon(bestand),
                            punkte
                    );
                })
                .filter(v -> !v.punkte().isEmpty())
                .toList();
    }


    // =========================================================================
    // Verwaltung: Mitglieder
    // =========================================================================

    public List<Mitglied> mitglieder() {
        Zeitachse zeit = zeitachse();

        Map<String, MitgliedSammler> sammler = new HashMap<>();

        for (BestellungEntity b : alleBestellungen()) {
            if (b.getPersonId() == null) {
                continue;
            }

            MitgliedSammler s = sammler.computeIfAbsent(b.getPersonId(), MitgliedSammler::new);
            s.bestellungen++;
            s.aktivitaet(b.getDatum());

            DeadlineEntity runde = zeit.rundeVon(b);
            if (runde != null) {
                s.runden.add(runde.getId());
            }
        }

        for (EinkaufEntity e : einkaufRepository.alle()) {
            if (e.getPersonId() == null) {
                continue;
            }

            MitgliedSammler s = sammler.computeIfAbsent(e.getPersonId(), MitgliedSammler::new);
            s.einkaeufe++;
            s.ausgegeben += e.getTotalPriceAtTime() + e.getDeliveryCostAtTime();
            s.aktivitaet(e.getDate());
        }

        // Namen aus Keycloak – optional, die Übersicht funktioniert auch ohne.
        Map<String, User> benutzer = MitgliederNamen.laden(keycloak);

        benutzer.keySet().forEach(username ->
                sammler.computeIfAbsent(username, MitgliedSammler::new)
        );

        return sammler.values()
                .stream()
                .map(s -> {
                    User user = benutzer.get(s.personId);

                    return new Mitglied(
                            s.personId,
                            MitgliederNamen.anzeigename(user),
                            user == null ? null : user.email(),
                            s.bestellungen,
                            s.einkaeufe,
                            s.runden.size(),
                            runden2(s.ausgegeben),
                            s.letzteAktivitaet
                    );
                })
                .sorted(Comparator
                        .comparing(Mitglied::letzteAktivitaet, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Mitglied::personId, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }


    private static final class MitgliedSammler {
        final String personId;
        int bestellungen;
        int einkaeufe;
        double ausgegeben;
        final Set<String> runden = new HashSet<>();
        LocalDateTime letzteAktivitaet;

        MitgliedSammler(String personId) {
            this.personId = personId;
        }

        void aktivitaet(LocalDateTime zeitpunkt) {
            if (zeitpunkt != null
                    && (letzteAktivitaet == null || zeitpunkt.isAfter(letzteAktivitaet))) {
                letzteAktivitaet = zeitpunkt;
            }
        }
    }


    // =========================================================================
    // Verwaltung: Statistik
    // =========================================================================

    public Statistik statistik(int anzahlRunden) {
        Zeitachse zeit = zeitachse();

        List<DeadlineEntity> runden =
                zeit.absteigend()
                        .stream()
                        .limit(Math.max(1, anzahlRunden))
                        .toList();

        Set<String> rundenIds =
                runden.stream().map(DeadlineEntity::getId).collect(Collectors.toSet());

        Map<String, RundenSammler> sammler = new LinkedHashMap<>();
        runden.forEach(d -> sammler.put(d.getId(), new RundenSammler()));

        Map<String, ProduktSammler> produkte = new HashMap<>();

        for (BestellungEntity b : alleBestellungen()) {
            DeadlineEntity runde = zeit.rundeVon(b);

            if (runde == null || !rundenIds.contains(runde.getId())) {
                continue;
            }

            RundenSammler s = sammler.get(runde.getId());
            s.bestellungen++;
            if (b.getPersonId() != null) {
                s.personen.add(b.getPersonId());
            }

            BestandEntity bestand = bestandVon(b);
            if (bestand != null) {
                ProduktSammler p = produkte.computeIfAbsent(
                        bestand.getId(),
                        id -> new ProduktSammler(bestand, b instanceof BrotBestellung ? "BROT" : "FRISCH")
                );
                p.bestellungen++;
                if (!istSpezialfall(bestand)) {
                    p.menge += b.getBestellmenge();
                }
                if (b.getPersonId() != null) {
                    p.besteller.add(b.getPersonId());
                }
            }
        }

        for (EinkaufEntity e : einkaufRepository.alle()) {
            DeadlineEntity runde = zeit.rundeVon(e);

            if (runde == null || !rundenIds.contains(runde.getId())) {
                continue;
            }

            RundenSammler s = sammler.get(runde.getId());
            s.einkaeufe++;
            s.frisch += e.getFreshPriceAtTime();
            s.brot += e.getBreadPriceAtTime();
            s.lager += e.getBestandPriceAtTime();
            s.zuViel += e.getTooMuchPriceAtTime();
            s.liefer += e.getDeliveryCostAtTime();
            if (e.getPersonId() != null) {
                s.personen.add(e.getPersonId());
            }

            // Umsatz je Produkt (aktueller Preis als Näherung)
            for (BestellungBuyEntity item : liste(e.getBestellungsEinkauf())) {
                BestandEntity bestand = item.getBestellung() == null ? null : bestandVon(item.getBestellung());
                umsatz(produkte, bestand, item.getBestellung() instanceof BrotBestellung ? "BROT" : "FRISCH", item.getAmount());
            }

            for (BestandBuyEntity item : liste(e.getBestandEinkauf())) {
                ProduktSammler p = umsatz(produkte, item.getBestand(), "LAGER", item.getAmount());
                if (p != null) {
                    p.menge += item.getAmount();
                    if (e.getPersonId() != null) {
                        p.besteller.add(e.getPersonId());
                    }
                }
            }
        }

        List<RundenStatistik> rundenStatistik =
                runden.stream()
                        .map(d -> {
                            RundenSammler s = sammler.get(d.getId());
                            double gesamt = s.frisch + s.brot + s.lager + s.zuViel + s.liefer;

                            return new RundenStatistik(
                                    d.getId(),
                                    d.getDatum(),
                                    zeit.ende(d),
                                    zeit.status(d),
                                    runden2(s.frisch),
                                    runden2(s.brot),
                                    runden2(s.lager),
                                    runden2(s.zuViel),
                                    runden2(s.liefer),
                                    runden2(gesamt),
                                    s.personen.size(),
                                    s.bestellungen,
                                    s.einkaeufe
                            );
                        })
                        .toList();

        List<ProduktStatistik> topProdukte =
                produkte.values()
                        .stream()
                        .map(p -> new ProduktStatistik(
                                p.bestand.getId(),
                                produktName(p.bestand),
                                p.typ,
                                einheitVon(p.bestand),
                                p.bestellungen,
                                p.besteller.size(),
                                runden3(p.menge),
                                runden2(p.umsatz)
                        ))
                        .sorted(Comparator
                                .comparingInt(ProduktStatistik::bestellungen).reversed()
                                .thenComparing(Comparator.comparingDouble(ProduktStatistik::umsatz).reversed()))
                        .limit(20)
                        .toList();

        return new Statistik(
                rundenStatistik,
                topProdukte,
                mitglieder().size()
        );
    }


    private static ProduktSammler umsatz(
            Map<String, ProduktSammler> produkte,
            BestandEntity bestand,
            String typ,
            double menge
    ) {
        if (bestand == null) {
            return null;
        }

        ProduktSammler p = produkte.computeIfAbsent(bestand.getId(), id -> new ProduktSammler(bestand, typ));

        if (bestand.getPreis() != null) {
            p.umsatz += bestand.getPreis().doubleValue() * menge;
        }

        return p;
    }


    private static final class RundenSammler {
        int bestellungen;
        int einkaeufe;
        double frisch;
        double brot;
        double lager;
        double zuViel;
        double liefer;
        final Set<String> personen = new HashSet<>();
    }


    private static final class ProduktSammler {
        final BestandEntity bestand;
        final String typ;
        int bestellungen;
        double menge;
        double umsatz;
        final Set<String> besteller = new HashSet<>();

        ProduktSammler(BestandEntity bestand, String typ) {
            this.bestand = bestand;
            this.typ = typ;
        }
    }


    private Zeitachse zeitachse() {
        return Zeitachse.laden(deadlineRepository, deadlineService);
    }



    // =========================================================================
    // Preise (Snapshot je Runde, Fallback aktueller Preis)
    // =========================================================================

    private final class Preise {

        private final Map<String, Map<String, BigDecimal>> jeRunde = new HashMap<>();

        BigDecimal preis(BestandEntity bestand, DeadlineEntity runde) {
            if (bestand == null) {
                return null;
            }

            if (runde != null) {
                BigDecimal snapshot =
                        jeRunde.computeIfAbsent(runde.getId(), this::laden)
                                .get(bestand.getId());

                if (snapshot != null) {
                    return snapshot;
                }
            }

            return bestand.getPreis();
        }

        /** Nur ID -> Preis: lädt die Bestände nicht einzeln nach. */
        private Map<String, BigDecimal> laden(String deadlineId) {
            return preisRepository.findePreiseVonDeadline(deadlineId);
        }
    }


    // =========================================================================
    // Hilfsfunktionen
    // =========================================================================

    private List<BestellungEntity> alleBestellungen() {
        List<BestellungEntity> alle = new ArrayList<>();
        alle.addAll(frischRepository.alle());
        alle.addAll(brotRepository.alle());
        return alle;
    }
}
