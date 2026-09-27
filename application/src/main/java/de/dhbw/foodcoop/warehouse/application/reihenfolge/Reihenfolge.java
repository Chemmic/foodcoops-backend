package de.dhbw.foodcoop.warehouse.application.reihenfolge;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import de.dhbw.foodcoop.warehouse.domain.entities.Sortierbar;

/**
 * ============================================================================
 * Reihenfolge von Produktlisten (Frisch, Brot, Lager)
 * ============================================================================
 *
 * Jedes Produkt hat einen Platz (1 = oben). Gespeichert werden nur Produkte,
 * deren Platz sich wirklich ändert.
 *
 * ============================================================================
 */
public final class Reihenfolge {

    private Reihenfolge() {
    }


    /** Nach Platz, dann Name; Produkte ohne Platz ans Ende. */
    public static <T extends Sortierbar> Comparator<T> vergleich() {
        return Comparator
                .comparing((T t) -> t.getSortierung(), Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(t -> t.getName() == null ? "" : t.getName(), String.CASE_INSENSITIVE_ORDER);
    }


    public static <T extends Sortierbar> List<T> sortiert(List<T> liste) {
        return liste.stream()
                .sorted(vergleich())
                .toList();
    }


    /**
     * Neues Produkt an den gewünschten Platz setzen (1 = oben); ohne Angabe
     * oder zu groß: ans Ende. Die folgenden rutschen eins nach unten.
     *
     * @param sortiert die bisherigen Produkte in Reihenfolge (ohne das neue)
     */
    public static <T extends Sortierbar> void einordnen(List<T> sortiert, T neu, Consumer<T> speichern) {
        int platz =
                neu.getSortierung() == null || neu.getSortierung() > sortiert.size()
                        ? sortiert.size() + 1
                        : Math.max(1, neu.getSortierung());

        for (int i = 0; i < sortiert.size(); i++) {
            int soll = i + 1 < platz ? i + 1 : i + 2;
            setzeFallsGeaendert(sortiert.get(i), soll, speichern);
        }

        neu.setSortierung(platz);
    }


    /**
     * Reihenfolge übernehmen (z.B. nach Drag & Drop). Unbekannte IDs werden
     * ignoriert, nicht genannte Produkte bleiben in ihrer bisherigen
     * Reihenfolge dahinter.
     */
    public static <T extends Sortierbar> List<T> setzen(List<T> sortiert, List<String> ids, Consumer<T> speichern) {
        Map<String, T> nachId = new LinkedHashMap<>();
        sortiert.forEach(t -> nachId.put(t.getId(), t));

        List<T> neu = new ArrayList<>();

        if (ids != null) {
            for (String id : new LinkedHashSet<>(ids)) {
                T t = nachId.remove(id);
                if (t != null) {
                    neu.add(t);
                }
            }
        }

        neu.addAll(nachId.values());

        for (int i = 0; i < neu.size(); i++) {
            setzeFallsGeaendert(neu.get(i), i + 1, speichern);
        }

        return neu;
    }


    /**
     * Einmalig: Produkte ohne Platz bekommen einen – in der Reihenfolge, in
     * der sie bisher angezeigt wurden (Reihenfolge der Datenbank).
     *
     * @return Anzahl neu nummerierter Produkte
     */
    public static <T extends Sortierbar> int initialisieren(List<T> roh, Consumer<T> speichern) {
        if (roh.stream().allMatch(t -> t.getSortierung() != null)) {
            return 0;
        }

        // Stabil sortieren: Produkte ohne Platz behalten ihre bisherige Reihenfolge
        List<T> liste = new ArrayList<>(roh);
        liste.sort(Comparator.comparing((T t) -> t.getSortierung(), Comparator.nullsLast(Comparator.naturalOrder())));

        int geaendert = 0;

        for (int i = 0; i < liste.size(); i++) {
            if (setzeFallsGeaendert(liste.get(i), i + 1, speichern)) {
                geaendert++;
            }
        }

        return geaendert;
    }


    private static <T extends Sortierbar> boolean setzeFallsGeaendert(T t, int platz, Consumer<T> speichern) {
        if (t.getSortierung() != null && t.getSortierung() == platz) {
            return false;
        }

        t.setSortierung(platz);
        speichern.accept(t);
        return true;
    }
}
