package de.dhbw.foodcoop.warehouse.plugins.email;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Locale;

import de.dhbw.foodcoop.warehouse.domain.entities.EinkaufEntity;

/**
 * Beträge eines Einkaufs nach Bereichen – für den Platzhalter
 * %kostenUebersicht% in den Mails, als Text und als HTML-Tabelle.
 */
public record KostenUebersicht(
        double frisch,
        double brot,
        double lager,
        double zuViel,
        double lieferkosten,
        double gesamt
) {

    private record Zeile(String bereich, double betrag) {
    }


    public static KostenUebersicht aus(EinkaufEntity einkauf) {
        return new KostenUebersicht(
                runden(einkauf.getFreshPriceAtTime()),
                runden(einkauf.getBreadPriceAtTime()),
                runden(einkauf.getBestandPriceAtTime()),
                runden(einkauf.getTooMuchPriceAtTime()),
                runden(einkauf.getDeliveryCostAtTime()),
                runden(einkauf.getDeliveryCostAtTime() + einkauf.getTotalPriceAtTime())
        );
    }


    private static double runden(double wert) {
        return BigDecimal.valueOf(wert)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();
    }


    /** Betrag ohne Währung, z.B. "1.234,50". */
    public static String zahl(double betrag) {
        DecimalFormat format = new DecimalFormat(
                "#,##0.00",
                DecimalFormatSymbols.getInstance(Locale.GERMANY));

        return format.format(betrag);
    }


    /** Betrag mit Währung, z.B. "12,40 €". */
    public static String euro(double betrag) {
        return zahl(betrag) + " €";
    }


    private List<Zeile> zeilen() {
        return List.of(
                new Zeile("Frisch", frisch),
                new Zeile("Brot", brot),
                new Zeile("Lager", lager),
                new Zeile("Zu viel (Restmengen)", zuViel),
                new Zeile("Lieferkosten", lieferkosten)
        );
    }


    /** Untereinander, Beträge rechtsbündig (in Schriften fester Breite). */
    public String alsText() {
        List<Zeile> zeilen = zeilen();

        int breiteBereich = "Gesamt".length();
        int breiteBetrag = euro(gesamt).length();

        for (Zeile zeile : zeilen) {
            breiteBereich = Math.max(breiteBereich, zeile.bereich().length());
            breiteBetrag = Math.max(breiteBetrag, euro(zeile.betrag()).length());
        }

        String muster = "  %-" + breiteBereich + "s   %" + breiteBetrag + "s";

        StringBuilder text = new StringBuilder();

        for (Zeile zeile : zeilen) {
            text.append(String.format(muster, zeile.bereich(), euro(zeile.betrag())))
                    .append('\n');
        }

        text.append("  ")
                .append("-".repeat(breiteBereich + 3 + breiteBetrag))
                .append('\n')
                .append(String.format(muster, "Gesamt", euro(gesamt)));

        return text.toString();
    }


    /** Kleine Tabelle mit Inline-Styles (Mailprogramme ignorieren CSS-Dateien). */
    public String alsHtml() {
        StringBuilder html = new StringBuilder(
                "<table cellpadding=\"0\" cellspacing=\"0\" style=\"border-collapse:collapse;margin:8px 0 12px;min-width:260px\">");

        for (Zeile zeile : zeilen()) {
            String farbe = zeile.betrag() == 0 ? "#9e9e9e" : "#212121";

            html.append("<tr>")
                    .append("<td style=\"padding:4px 24px 4px 0;color:").append(farbe).append("\">")
                    .append(zeile.bereich())
                    .append("</td>")
                    .append("<td style=\"padding:4px 0;text-align:right;white-space:nowrap;color:").append(farbe).append("\">")
                    .append(euro(zeile.betrag()))
                    .append("</td>")
                    .append("</tr>");
        }

        html.append("<tr>")
                .append("<td style=\"padding:8px 24px 4px 0;border-top:2px solid #212121;font-weight:700\">Gesamt</td>")
                .append("<td style=\"padding:8px 0 4px;border-top:2px solid #212121;text-align:right;white-space:nowrap;font-weight:700\">")
                .append(euro(gesamt))
                .append("</td>")
                .append("</tr>")
                .append("</table>");

        return html.toString();
    }
}
