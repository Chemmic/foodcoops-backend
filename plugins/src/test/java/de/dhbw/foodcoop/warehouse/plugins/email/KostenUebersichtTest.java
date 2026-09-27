package de.dhbw.foodcoop.warehouse.plugins.email;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KostenUebersichtTest {

    private final KostenUebersicht kosten =
            new KostenUebersicht(12.4, 4.5, 1234.5, 0, 0.8, 1252.2);


    @Test
    void betraegeDeutschFormatiert() {
        assertThat(KostenUebersicht.euro(12.4)).isEqualTo("12,40 €");
        assertThat(KostenUebersicht.zahl(1234.5)).isEqualTo("1.234,50");
    }


    @Test
    void textMitAllenBereichenUndGesamt() {
        assertThat(kosten.alsText()).isEqualTo(
                "  Frisch                    12,40 €\n"
                + "  Brot                       4,50 €\n"
                + "  Lager                  1.234,50 €\n"
                + "  Zu viel (Restmengen)       0,00 €\n"
                + "  Lieferkosten               0,80 €\n"
                + "  ---------------------------------\n"
                + "  Gesamt                 1.252,20 €");
    }


    @Test
    void htmlTabelleMitGesamt() {
        assertThat(kosten.alsHtml())
                .startsWith("<table")
                .contains("Lieferkosten", "0,80 €", "Gesamt", "1.252,20 €");
    }
}
