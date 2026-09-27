package de.dhbw.foodcoop.warehouse.plugins.historie;

import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.einheitVon;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.istSpezialfall;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.liste;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.produktName;
import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.runden3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestandEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellUebersicht;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestellung;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.DiscrepancyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestand;
import de.dhbw.foodcoop.warehouse.domain.entities.FrischBestellung;
import de.dhbw.foodcoop.warehouse.domain.repositories.BestellÜbersichtRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestandRepository;
import de.dhbw.foodcoop.warehouse.domain.repositories.FrischBestellungRepository;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.BrotPosition;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.FinaleBestellung;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.Kategorie;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.Position;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.RundenStand;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.Verlauf;

/**
 * ============================================================================
 * Finale Bestellung festlegen (Rolle Organisator)
 * ============================================================================
 *
 * Bearbeitet wird immer die Bestellübersicht der letzten Deadline. Sie wird
 * beim Anlegen einer neuen Deadline erzeugt und enthält die Bestellungen der
 * Runde davor. Beispiel mit Deadlines am 19. und 26.:
 *
 *   bis zum 26.   -> Bestellungen bis zum 19. (Übersicht an der 19.-Deadline)
 *   ab dem 26.    -> Bestellungen vom 19. bis 26.
 *
 * Geändert werden nur die Gebinde der Übersicht (= was beim Händler bestellt
 * wird und im PDF landet). Bestellungen der Mitglieder bleiben unangetastet.
 *
 * zuVielZuWenig = Gebinde * Gebindegröße - gewollte Menge
 * (wie beim bisherigen Ändern der Gebinde).
 *
 * ============================================================================
 */
@Service
@Transactional(readOnly = true)
public class FinaleBestellungService {

    private static final Logger LOG =
            LoggerFactory.getLogger(FinaleBestellungService.class);

    private static final double EPSILON = 0.0005;
    private static final double MAX_GEBINDE = 999;


    private final BestellÜbersichtRepository uebersichten;
    private final DeadlineRepository deadlineRepository;
    private final DeadlineService deadlineService;
    private final FrischBestellungRepository frischRepository;
    private final FrischBestandRepository bestandRepository;


    public FinaleBestellungService(
            BestellÜbersichtRepository uebersichten,
            DeadlineRepository deadlineRepository,
            DeadlineService deadlineService,
            FrischBestellungRepository frischRepository,
            FrischBestandRepository bestandRepository
    ) {
        this.uebersichten = uebersichten;
        this.deadlineRepository = deadlineRepository;
        this.deadlineService = deadlineService;
        this.frischRepository = frischRepository;
        this.bestandRepository = bestandRepository;
    }


    // =========================================================================
    // Lesen
    // =========================================================================

    public FinaleBestellung laden(int verlaufRunden) {
        Zeitachse zeit = Zeitachse.laden(deadlineRepository, deadlineService);
        List<DeadlineEntity> absteigend = zeit.absteigend();

        if (absteigend.isEmpty()) {
            return new FinaleBestellung(null, null, null, null, List.of(), List.of(), List.of());
        }

        DeadlineEntity letzte = absteigend.get(0);
        DeadlineEntity runde = zeit.einkaufsRunde();
        BestellUebersicht uebersicht = uebersichtVon(letzte);

        // Vorherige Runden: Übersichten der Deadlines davor, neueste zuerst
        List<DeadlineEntity> vorher =
                absteigend.subList(1, Math.min(absteigend.size(), 1 + Math.max(0, verlaufRunden)));

        List<Map<String, RundenStand>> verlauf =
                vorher.stream()
                        .map(d -> staende(uebersichtVon(d)))
                        .toList();

        Map<String, Integer> besteller =
                runde == null ? Map.of() : bestellerJeProdukt(zeit, runde);

        List<Kategorie> kategorien =
                uebersicht == null
                        ? List.of()
                        : kategorien(uebersicht, besteller, verlauf);

        List<BrotPosition> brot =
                uebersicht == null
                        ? List.of()
                        : brot(uebersicht);

        return new FinaleBestellung(
                uebersicht == null ? null : uebersicht.getId(),
                runde == null ? null : runde.getDatum(),
                letzte.getDatum(),
                zeit.ende(letzte),
                vorher.stream().map(DeadlineEntity::getDatum).toList(),
                kategorien,
                brot
        );
    }


    private List<Kategorie> kategorien(
            BestellUebersicht uebersicht,
            Map<String, Integer> besteller,
            List<Map<String, RundenStand>> verlauf
    ) {
        Map<String, List<DiscrepancyEntity>> jeProdukt = jeProdukt(uebersicht);

        Map<String, List<Position>> gruppen = new LinkedHashMap<>();
        Map<String, Boolean> mischbar = new HashMap<>();

        jeProdukt.values()
                .stream()
                .map(eintraege -> position(eintraege, besteller, verlauf))
                .sorted(Comparator.comparing(Position::produkt, String.CASE_INSENSITIVE_ORDER))
                .forEach(position -> {
                    BestandEntity bestand = jeProdukt.get(position.produktId()).get(0).getBestand();
                    String name = kategorieName(bestand);

                    gruppen.computeIfAbsent(name, k -> new ArrayList<>()).add(position);
                    mischbar.merge(name, istMischbar(bestand), Boolean::logicalOr);
                });

        return gruppen.entrySet()
                .stream()
                .map(e -> new Kategorie(e.getKey(), mischbar.get(e.getKey()), e.getValue()))
                .sorted(Comparator
                        .comparing((Kategorie k) -> !k.mischbar())
                        .thenComparing(Kategorie::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }


    private static Position position(
            List<DiscrepancyEntity> eintraege,
            Map<String, Integer> besteller,
            List<Map<String, RundenStand>> verlauf
    ) {
        BestandEntity bestand = eintraege.get(0).getBestand();

        double gewollt = eintraege.stream().mapToDouble(DiscrepancyEntity::getGewollteMenge).sum();
        double gebinde = eintraege.stream().mapToDouble(DiscrepancyEntity::getZuBestellendeGebinde).sum();
        double differenz = eintraege.stream().mapToDouble(DiscrepancyEntity::getZuVielzuWenig).sum();

        List<RundenStand> runden =
                verlauf.stream()
                        .map(stand -> stand.get(bestand.getId()))
                        .toList();

        int gekauftInFolge = 0;
        while (gekauftInFolge < runden.size()
                && runden.get(gekauftInFolge) != null
                && runden.get(gekauftInFolge).gekauft()) {
            gekauftInFolge++;
        }

        int gewuenschtInFolge = 0;
        int davonOhneGebinde = 0;

        if (gewollt > EPSILON) {
            gewuenschtInFolge = 1;

            for (RundenStand stand : runden) {
                if (stand == null || !stand.gewuenscht()) {
                    break;
                }

                gewuenschtInFolge++;

                if (!stand.gekauft()) {
                    davonOhneGebinde++;
                }
            }
        }

        return new Position(
                bestand.getId(),
                produktName(bestand),
                einheitVon(bestand),
                istSpezialfall(bestand),
                gebindegroesse(bestand),
                runden3(gewollt),
                runden3(gebinde),
                runden3(differenz),
                besteller.getOrDefault(bestand.getId(), 0),
                new Verlauf(runden, gekauftInFolge, gewuenschtInFolge, davonOhneGebinde)
        );
    }


    private static List<BrotPosition> brot(BestellUebersicht uebersicht) {
        Map<String, List<BrotBestellung>> jeProdukt =
                liste(uebersicht.getBrotBestellung())
                        .stream()
                        .filter(b -> b.getBrotBestand() != null)
                        .collect(Collectors.groupingBy(b -> b.getBrotBestand().getId()));

        return jeProdukt.values()
                .stream()
                .map(bestellungen -> new BrotPosition(
                        bestellungen.get(0).getBrotBestand().getId(),
                        produktName(bestellungen.get(0).getBrotBestand()),
                        runden3(bestellungen.stream().mapToDouble(BrotBestellung::getBestellmenge).sum()),
                        (int) bestellungen.stream()
                                .map(BrotBestellung::getPersonId)
                                .filter(p -> p != null)
                                .distinct()
                                .count()
                ))
                .filter(b -> b.menge() > EPSILON)
                .sorted(Comparator.comparing(BrotPosition::produkt, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }


    /** Je Produkt: gewünscht (gewollte Menge) und gekauft (Gebinde > 0). */
    private static Map<String, RundenStand> staende(BestellUebersicht uebersicht) {
        if (uebersicht == null) {
            return Map.of();
        }

        Map<String, RundenStand> ergebnis = new HashMap<>();

        jeProdukt(uebersicht).forEach((id, eintraege) -> ergebnis.put(id, new RundenStand(
                eintraege.stream().mapToDouble(DiscrepancyEntity::getGewollteMenge).sum() > EPSILON,
                eintraege.stream().mapToDouble(DiscrepancyEntity::getZuBestellendeGebinde).sum() > EPSILON
        )));

        return ergebnis;
    }


    /** Anzahl verschiedener Personen je Frisch-Produkt in der Runde. */
    private Map<String, Integer> bestellerJeProdukt(Zeitachse zeit, DeadlineEntity runde) {
        Map<String, Set<String>> personen = new HashMap<>();

        for (FrischBestellung b : frischRepository.alle()) {
            if (b.getPersonId() == null || b.getFrischbestand() == null) {
                continue;
            }

            DeadlineEntity r = zeit.rundeVon(b);

            if (r != null && r.getId().equals(runde.getId())) {
                personen.computeIfAbsent(b.getFrischbestand().getId(), k -> new HashSet<>())
                        .add(b.getPersonId());
            }
        }

        return personen.entrySet()
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().size()));
    }


    // =========================================================================
    // Ändern
    // =========================================================================

    @Transactional
    public Position gebindeSetzen(String produktId, Double gebinde) {
        double anzahl = pruefeGebinde(gebinde);
        BestellUebersicht uebersicht = aktuelleUebersicht();

        List<DiscrepancyEntity> eintraege = jeProdukt(uebersicht).get(produktId);

        if (eintraege == null) {
            throw new FinaleBestellungException(
                    HttpStatus.NOT_FOUND,
                    "Das Produkt ist nicht in der Bestellung."
            );
        }

        // Alte Daten können mehrere Einträge je Produkt haben: alles auf den ersten
        for (int i = 0; i < eintraege.size(); i++) {
            DiscrepancyEntity eintrag = eintraege.get(i);
            eintrag.setZuBestellendeGebinde(i == 0 ? anzahl : 0);
            eintrag.setZuVielzuWenig(differenz(eintrag));
        }

        uebersichten.speichern(uebersicht);

        return antwort(produktId);
    }


    @Transactional
    public Position hinzufuegen(String produktId, Double gebinde) {
        double anzahl = gebinde == null ? 1 : pruefeGebinde(gebinde);

        if (produktId == null || produktId.isBlank()) {
            throw new FinaleBestellungException(HttpStatus.BAD_REQUEST, "Bitte ein Produkt auswählen.");
        }

        BestellUebersicht uebersicht = aktuelleUebersicht();

        if (jeProdukt(uebersicht).containsKey(produktId)) {
            throw new FinaleBestellungException(
                    HttpStatus.CONFLICT,
                    "Das Produkt ist schon in der Bestellung – ändere dort die Gebinde."
            );
        }

        FrischBestand bestand = bestandRepository.findeMitId(produktId)
                .orElseThrow(() -> new FinaleBestellungException(
                        HttpStatus.NOT_FOUND,
                        "Das Produkt gibt es nicht."
                ));

        DiscrepancyEntity eintrag =
                new DiscrepancyEntity(UUID.randomUUID().toString(), bestand, anzahl, 0f, 0f);
        eintrag.setZuVielzuWenig(differenz(eintrag));

        if (uebersicht.getDiscrepancy() == null) {
            uebersicht.setDiscrepancy(new ArrayList<>());
        }

        uebersicht.getDiscrepancy().add(eintrag);
        uebersichten.speichern(uebersicht);

        return antwort(produktId);
    }


    /** Nur nachträglich ergänzte Produkte (ohne Bestellungen) lassen sich entfernen. */
    @Transactional
    public void entfernen(String produktId) {
        BestellUebersicht uebersicht = aktuelleUebersicht();
        List<DiscrepancyEntity> eintraege = jeProdukt(uebersicht).get(produktId);

        if (eintraege == null) {
            throw new FinaleBestellungException(
                    HttpStatus.NOT_FOUND,
                    "Das Produkt ist nicht in der Bestellung."
            );
        }

        if (eintraege.stream().mapToDouble(DiscrepancyEntity::getGewollteMenge).sum() > EPSILON) {
            throw new FinaleBestellungException(
                    HttpStatus.CONFLICT,
                    "Das Produkt wurde von Mitgliedern bestellt. Setze die Gebinde auf 0, wenn es nicht bestellt werden soll."
            );
        }

        uebersicht.getDiscrepancy().removeAll(eintraege);
        uebersichten.speichern(uebersicht);
    }


    /** Position nach dem Speichern (ohne Verlauf/Besteller – die ändern sich nicht). */
    private Position antwort(String produktId) {
        List<DiscrepancyEntity> eintraege = jeProdukt(aktuelleUebersicht()).get(produktId);
        return position(eintraege, Map.of(), List.of());
    }


    private BestellUebersicht aktuelleUebersicht() {
        DeadlineEntity letzte = deadlineRepository.letzte()
                .orElseThrow(() -> new FinaleBestellungException(
                        HttpStatus.NOT_FOUND,
                        "Es gibt noch keine Bestellrunde."
                ));

        BestellUebersicht uebersicht = uebersichtVon(letzte);

        if (uebersicht == null) {
            throw new FinaleBestellungException(
                    HttpStatus.NOT_FOUND,
                    "Für diese Runde gibt es noch keine Bestellübersicht."
            );
        }

        return uebersicht;
    }


    private static double pruefeGebinde(Double gebinde) {
        if (gebinde == null || gebinde.isNaN() || gebinde < 0 || gebinde > MAX_GEBINDE) {
            throw new FinaleBestellungException(
                    HttpStatus.BAD_REQUEST,
                    "Bitte eine Anzahl Gebinde zwischen 0 und 999 angeben."
            );
        }

        return runden3(gebinde);
    }


    // =========================================================================
    // Hilfsfunktionen
    // =========================================================================

    private BestellUebersicht uebersichtVon(DeadlineEntity deadline) {
        try {
            return uebersichten.findeMitDeadline(deadline);
        } catch (RuntimeException exception) {
            // z.B. mehrere Übersichten zu einer Deadline
            LOG.warn("Bestellübersicht zu Deadline {} nicht lesbar: {}", deadline.getId(), exception.getMessage());
            return null;
        }
    }


    private static Map<String, List<DiscrepancyEntity>> jeProdukt(BestellUebersicht uebersicht) {
        return liste(uebersicht.getDiscrepancy())
                .stream()
                .filter(d -> d.getBestand() != null)
                .collect(Collectors.groupingBy(d -> d.getBestand().getId(), LinkedHashMap::new, Collectors.toList()));
    }


    private static float differenz(DiscrepancyEntity eintrag) {
        return (float) runden3(
                eintrag.getZuBestellendeGebinde() * gebindegroesse(eintrag.getBestand())
                        - eintrag.getGewollteMenge()
        );
    }


    private static double gebindegroesse(BestandEntity bestand) {
        return bestand instanceof FrischBestand frisch
                ? frisch.getGebindegroesse()
                : 1;
    }


    private static boolean istMischbar(BestandEntity bestand) {
        return bestand instanceof FrischBestand frisch
                && frisch.getKategorie() != null
                && frisch.getKategorie().isMixable();
    }


    private static String kategorieName(BestandEntity bestand) {
        return bestand instanceof FrischBestand frisch && frisch.getKategorie() != null
                ? frisch.getKategorie().getName()
                : "Ohne Kategorie";
    }


    public static class FinaleBestellungException extends RuntimeException {

        private final HttpStatus status;

        public FinaleBestellungException(HttpStatus status, String message) {
            super(message);
            this.status = status;
        }

        public HttpStatus getStatus() {
            return status;
        }
    }
}
