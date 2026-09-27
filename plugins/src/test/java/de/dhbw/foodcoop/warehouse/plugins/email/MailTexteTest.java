package de.dhbw.foodcoop.warehouse.plugins.email;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MailTexteTest {

    private static final LocalDate TAG = LocalDate.of(2026, 9, 27);


    @Test
    void fillsPlaceholders() {
        MailTexte texte = new MailTexte(
                "FoodCoop Nordstadt",
                "Einkauf %datum% – %foodcoop%",
                "%person% hat bei der %foodcoop% eingekauft"
        );

        assertThat(texte.betreffEinkauf("anna", TAG))
                .isEqualTo("Einkauf 27.09.2026 – FoodCoop Nordstadt");
        assertThat(texte.betreffEinkaufsmanagement("anna", TAG))
                .isEqualTo("anna hat bei der FoodCoop Nordstadt eingekauft");
        assertThat(texte.betreff("Bestellübersicht vom 27-09-2026"))
                .isEqualTo("FoodCoop Nordstadt – Bestellübersicht vom 27-09-2026");
        assertThat(texte.gruss()).endsWith("Deine FoodCoop Nordstadt");
    }


    @Test
    void emptyValuesFallBackToDefaults() {
        MailTexte texte = new MailTexte(" ", "", null);

        assertThat(texte.name()).isEqualTo("FoodCoop");
        assertThat(texte.betreffEinkauf("anna", TAG))
                .isEqualTo("Dein Einkauf bei der FoodCoop am 27.09.2026");
        assertThat(texte.betreffEinkaufsmanagement(null, TAG))
                .isEqualTo("Einkauf von  bei der FoodCoop am 27.09.2026");
    }
}
