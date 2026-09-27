package de.dhbw.foodcoop.warehouse.plugins.migration;

import java.util.function.IntSupplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import de.dhbw.foodcoop.warehouse.application.admin.ConfigurationService;
import de.dhbw.foodcoop.warehouse.application.brot.BrotBestandService;
import de.dhbw.foodcoop.warehouse.application.frischbestellung.FrischBestandService;
import de.dhbw.foodcoop.warehouse.application.lager.LagerChargenService;
import de.dhbw.foodcoop.warehouse.application.lager.ProduktService;
import de.dhbw.foodcoop.warehouse.domain.entities.ConfigurationEntity;
import de.dhbw.foodcoop.warehouse.domain.utils.ConstantsUtils;

/**
 * ============================================================================
 * Einmalige Datenanpassungen beim Start (idempotent)
 * ============================================================================
 *
 *   Frischwaren, Brote und Lagerprodukte ohne Platz in der Liste bekommen einen – in der Reihenfolge,
 *   in der sie bisher angezeigt wurden.
 *
 *   Vorhandener Lagerbestand ohne Chargen wird zu einer Charge zum
 *   aktuellen Preis.
 *
 *   Ein unveränderter alter Standardtext der Mail ans Einkaufsmanagement
 *   wird durch den neuen (mit Kostenübersicht) ersetzt.
 *
 * Fehler verhindern den Start nicht – beide Schritte gleichen sich sonst
 * auch beim nächsten Ändern selbst ab.
 *
 * ============================================================================
 */
@Component
public class DatenMigration implements ApplicationRunner {

    private static final Logger LOG =
            LoggerFactory.getLogger(DatenMigration.class);


    private final FrischBestandService frischBestand;
    private final BrotBestandService brotBestand;
    private final ProduktService produkte;
    private final LagerChargenService chargen;
    private final ConfigurationService konfiguration;


    public DatenMigration(
            FrischBestandService frischBestand,
            BrotBestandService brotBestand,
            ProduktService produkte,
            LagerChargenService chargen,
            ConfigurationService konfiguration
    ) {
        this.frischBestand = frischBestand;
        this.brotBestand = brotBestand;
        this.produkte = produkte;
        this.chargen = chargen;
        this.konfiguration = konfiguration;
    }


    @Override
    public void run(ApplicationArguments args) {
        reihenfolge("Frischwaren", frischBestand::reihenfolgeInitialisieren);
        reihenfolge("Brote", brotBestand::reihenfolgeInitialisieren);
        reihenfolge("Lagerprodukte", produkte::reihenfolgeInitialisieren);

        try {
            int angelegt = chargen.migrieren(produkte.all());

            if (angelegt > 0) {
                LOG.info("Lagerbestand von {} Produkten als Charge übernommen.", angelegt);
            }
        } catch (RuntimeException exception) {
            LOG.warn("Lager-Chargen konnten nicht angelegt werden.", exception);
        }

        try {
            einkaufsmanagementText();
        } catch (RuntimeException exception) {
            LOG.warn("Mailtext fürs Einkaufsmanagement konnte nicht aktualisiert werden.", exception);
        }
    }


    private void einkaufsmanagementText() {
        ConfigurationEntity config =
                konfiguration.getConfig().orElse(null);

        if (config == null
                || !normal(config.getEinkaufsmanagementEmailText())
                        .equals(normal(ConstantsUtils.EMAIL_TEXT_EINKAUFSMANAGEMENT_ALT))) {
            return;
        }

        config.setEinkaufsmanagementEmailText(ConstantsUtils.EMAIL_TEXT_EINKAUFSMANAGEMENT);
        konfiguration.updateConfig(config);

        LOG.info("Mailtext fürs Einkaufsmanagement auf den neuen Standard (mit Kostenübersicht) gesetzt.");
    }


    /** Vergleich ohne Unterschiede bei Zeilenenden und Leerzeichen am Zeilenende. */
    private static String normal(String text) {
        return text == null
                ? ""
                : text.replace("\r\n", "\n").replaceAll("[ \\t]+\n", "\n").strip();
    }


    private static void reihenfolge(String was, IntSupplier initialisieren) {
        try {
            int nummeriert = initialisieren.getAsInt();

            if (nummeriert > 0) {
                LOG.info("Reihenfolge für {} {} festgelegt.", nummeriert, was);
            }
        } catch (RuntimeException exception) {
            LOG.warn("Reihenfolge der {} konnte nicht festgelegt werden.", was, exception);
        }
    }
}
