package de.dhbw.foodcoop.warehouse.plugins.historie;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Lesemodell für Bestellhistorie und Statistiken.
 *
 * Eine Bestellrunde entspricht einer Deadline: Bestellungen gehören zur
 * Runde, in der sie aufgegeben wurden. Einkäufe werden der Runde
 * zugeordnet, deren Bestellungen sie abholen.
 */
public final class HistorieDtos {

    private HistorieDtos() {
    }


    // =========================================================================
    // Persönliche Historie
    // =========================================================================

    public record Historie(
            String personId,
            List<Bestellrunde> runden,
            List<PreisVerlauf> preisVerlauf
    ) {
    }


    /**
     * status: AKTUELL (Bestellphase), EINKAUF (jetzt einkaufbar),
     *         ABGESCHLOSSEN
     */
    public record Bestellrunde(
            String deadlineId,
            LocalDateTime start,
            LocalDateTime ende,
            String status,
            List<BestellPosition> bestellungen,
            List<Einkauf> einkaeufe,
            double geschaetzterBestellwert,
            double ausgegeben
    ) {
    }


    /**
     * status: OFFEN (noch nicht eingekauft), GENOMMEN, NICHT_ABGEHOLT
     *
     * differenz = genommen - bestellt; null, wenn nicht vergleichbar
     * (z.B. Spezialfall: in Stück bestellt, in kg abgerechnet).
     */
    public record BestellPosition(
            String bestellungId,
            String typ,
            String produktId,
            String produkt,
            String einheit,
            String kategorie,
            double bestellt,
            Double genommen,
            Double differenz,
            boolean spezialfall,
            BigDecimal preis,
            String status
    ) {
    }


    public record Einkauf(
            String id,
            LocalDateTime datum,
            List<EinkaufPosition> positionen,
            double frisch,
            double brot,
            double lager,
            double zuViel,
            double lieferkosten,
            double gesamt
    ) {
    }


    /**
     * typ: FRISCH, BROT, LAGER, ZU_VIEL
     */
    public record EinkaufPosition(
            String typ,
            String produkt,
            String einheit,
            Double bestellt,
            double menge,
            BigDecimal preis,
            BigDecimal summe,
            boolean spezialfall
    ) {
    }


    public record PreisVerlauf(
            String produktId,
            String produkt,
            String einheit,
            List<PreisPunkt> punkte
    ) {
    }


    public record PreisPunkt(
            LocalDateTime datum,
            BigDecimal preis
    ) {
    }


    // =========================================================================
    // Verwaltung
    // =========================================================================

    public record Mitglied(
            String personId,
            String name,
            String email,
            int bestellungen,
            int einkaeufe,
            int runden,
            double ausgegeben,
            LocalDateTime letzteAktivitaet
    ) {
    }


    public record Statistik(
            List<RundenStatistik> runden,
            List<ProduktStatistik> topProdukte,
            int mitgliederGesamt
    ) {
    }


    public record RundenStatistik(
            String deadlineId,
            LocalDateTime start,
            LocalDateTime ende,
            String status,
            double frisch,
            double brot,
            double lager,
            double zuViel,
            double lieferkosten,
            double gesamt,
            int aktiveMitglieder,
            int bestellungen,
            int einkaeufe
    ) {
    }


    public record ProduktStatistik(
            String produktId,
            String produkt,
            String typ,
            String einheit,
            int bestellungen,
            int besteller,
            double menge,
            double umsatz
    ) {
    }
}
