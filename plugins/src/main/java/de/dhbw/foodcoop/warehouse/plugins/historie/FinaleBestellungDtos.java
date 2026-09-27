package de.dhbw.foodcoop.warehouse.plugins.historie;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Finale Bestellung beim Händler: was aus der letzten Bestellrunde
 * tatsächlich bestellt wird (Bestellübersicht / PDF).
 *
 * Enthält bewusst keine Personen – nur Mengen und Anzahl Besteller.
 */
public final class FinaleBestellungDtos {

    private FinaleBestellungDtos() {
    }


    public record FinaleBestellung(
            /** null, solange es für die Runde keine Bestellübersicht gibt. */
            String uebersichtId,
            /** Bestellungen von start bis ende werden hier festgelegt. */
            LocalDateTime start,
            LocalDateTime ende,
            /** Ab dann wird die nächste Runde festgelegt. */
            LocalDateTime naechsteRunde,
            /** Vorherige Runden (Ende der Runde), neueste zuerst. */
            List<LocalDateTime> verlaufRunden,
            List<Kategorie> kategorien,
            List<BrotPosition> brot
    ) {
    }


    public record Kategorie(
            String name,
            boolean mischbar,
            List<Position> positionen
    ) {
    }


    public record Position(
            String produktId,
            String produkt,
            String einheit,
            boolean spezialfall,
            double gebindegroesse,
            double gewollteMenge,
            double zuBestellendeGebinde,
            /** Positiv: zu viel, negativ: zu wenig. */
            double zuVielZuWenig,
            /** Anzahl Personen, die das Produkt in der Runde bestellt haben. */
            int besteller,
            Verlauf verlauf
    ) {
    }


    /**
     * runden: je vorheriger Runde (gleiche Reihenfolge wie verlaufRunden),
     * null = Produkt kam in der Runde nicht vor.
     */
    public record Verlauf(
            List<RundenStand> runden,
            /** Vorherige Runden in Folge, in denen das Produkt bestellt wurde. */
            int gekauftInFolge,
            /** Runden in Folge (inkl. dieser), in denen es gewünscht wurde. */
            int gewuenschtInFolge,
            /** Davon (ohne diese Runde): wie oft kam kein Gebinde zustande. */
            int davonOhneGebinde
    ) {
    }


    public record RundenStand(
            boolean gewuenscht,
            boolean gekauft
    ) {
    }


    public record BrotPosition(
            String produktId,
            String produkt,
            double menge,
            int besteller
    ) {
    }


    public record GebindeAenderung(
            Double gebinde
    ) {
    }


    public record NeuePosition(
            String produktId,
            Double gebinde
    ) {
    }
}
