package de.dhbw.foodcoop.warehouse.plugins.historie;

import static de.dhbw.foodcoop.warehouse.plugins.historie.HistorieHilfen.liste;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungBuyEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.BestellungEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.DeadlineEntity;
import de.dhbw.foodcoop.warehouse.domain.entities.EinkaufEntity;
import de.dhbw.foodcoop.warehouse.domain.repositories.DeadlineRepository;

/**
 * Deadlines als Bestellrunden, aufsteigend nach Datum.
 *
 *   letzte Deadline      -> AKTUELL   (es wird bestellt)
 *   vorletzte Deadline   -> EINKAUF   (Bestellschluss vorbei, wird geliefert
 *                                      und eingekauft)
 *   ältere               -> ABGESCHLOSSEN
 */
final class Zeitachse {

    private final List<DeadlineEntity> aufsteigend;
    private final Map<String, Integer> index = new HashMap<>();
    private final DeadlineService deadlineService;


    Zeitachse(List<DeadlineEntity> aufsteigend, DeadlineService deadlineService) {
        this.aufsteigend = aufsteigend;
        this.deadlineService = deadlineService;

        for (int i = 0; i < aufsteigend.size(); i++) {
            index.put(aufsteigend.get(i).getId(), i);
        }
    }


    static Zeitachse laden(DeadlineRepository repository, DeadlineService deadlineService) {
        return new Zeitachse(
                repository.alle()
                        .stream()
                        .filter(d -> d.getDatum() != null)
                        .sorted(Comparator.comparing(DeadlineEntity::getDatum))
                        .toList(),
                deadlineService
        );
    }


    List<DeadlineEntity> absteigend() {
        List<DeadlineEntity> liste = new ArrayList<>(aufsteigend);
        Collections.reverse(liste);
        return liste;
    }


    /** Die Runde mit dem Status EINKAUF (vorletzte Deadline), falls vorhanden. */
    DeadlineEntity einkaufsRunde() {
        return aufsteigend.size() < 2
                ? null
                : aufsteigend.get(aufsteigend.size() - 2);
    }


    /** Die {@code anzahl} Runden vor der angegebenen, neueste zuerst. */
    List<DeadlineEntity> vorher(DeadlineEntity runde, int anzahl) {
        Integer i = index.get(runde.getId());

        if (i == null) {
            return List.of();
        }

        List<DeadlineEntity> liste = new ArrayList<>();

        for (int j = i - 1; j >= 0 && liste.size() < anzahl; j--) {
            liste.add(aufsteigend.get(j));
        }

        return liste;
    }


    /** Die Deadline, die auf die Runde folgt (= ihr Bestellschluss). */
    DeadlineEntity naechste(DeadlineEntity runde) {
        Integer i = index.get(runde.getId());

        return i == null || i >= aufsteigend.size() - 1
                ? null
                : aufsteigend.get(i + 1);
    }


    /** Runde, die zum Zeitpunkt lief (letzte Deadline davor). */
    DeadlineEntity rundeZu(LocalDateTime zeitpunkt) {
        if (aufsteigend.isEmpty()) {
            return null;
        }

        if (zeitpunkt == null) {
            return aufsteigend.get(aufsteigend.size() - 1);
        }

        DeadlineEntity treffer = aufsteigend.get(0);

        for (DeadlineEntity d : aufsteigend) {
            if (d.getDatum().isAfter(zeitpunkt)) {
                break;
            }
            treffer = d;
        }

        return treffer;
    }


    DeadlineEntity rundeVon(BestellungEntity bestellung) {
        DeadlineEntity deadline = bestellung.getDeadline();

        if (deadline != null && index.containsKey(deadline.getId())) {
            return aufsteigend.get(index.get(deadline.getId()));
        }

        return rundeZu(bestellung.getDatum());
    }


    DeadlineEntity rundeVon(EinkaufEntity einkauf) {
        Map<String, Long> haeufigkeit =
                liste(einkauf.getBestellungsEinkauf())
                        .stream()
                        .map(BestellungBuyEntity::getBestellung)
                        .filter(Objects::nonNull)
                        .map(this::rundeVon)
                        .filter(Objects::nonNull)
                        .collect(Collectors.groupingBy(DeadlineEntity::getId, Collectors.counting()));

        if (!haeufigkeit.isEmpty()) {
            String id = haeufigkeit.entrySet()
                    .stream()
                    .max(Map.Entry.comparingByValue())
                    .get()
                    .getKey();

            return aufsteigend.get(index.get(id));
        }

        // Nur Lagerware / Zu viel: eingekauft wird die vorherige Runde
        DeadlineEntity laufend = rundeZu(einkauf.getDate());

        if (laufend == null) {
            return null;
        }

        int i = index.get(laufend.getId());
        return i > 0 ? aufsteigend.get(i - 1) : laufend;
    }


    String status(DeadlineEntity deadline) {
        int abstand = aufsteigend.size() - 1 - index.getOrDefault(deadline.getId(), 0);

        return switch (abstand) {
            case 0 -> "AKTUELL";
            case 1 -> "EINKAUF";
            default -> "ABGESCHLOSSEN";
        };
    }


    LocalDateTime ende(DeadlineEntity deadline) {
        DeadlineEntity naechste = naechste(deadline);

        if (naechste != null) {
            return naechste.getDatum();
        }

        try {
            return deadlineService.calculateDateFromDeadline(deadline);
        } catch (RuntimeException exception) {
            return null;
        }
    }
}
