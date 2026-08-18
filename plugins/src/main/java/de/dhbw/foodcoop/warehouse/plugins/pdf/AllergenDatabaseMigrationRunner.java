package de.dhbw.foodcoop.warehouse.plugins.pdf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;


public class AllergenDatabaseMigrationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(AllergenDatabaseMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public AllergenDatabaseMigrationRunner(
            JdbcTemplate jdbcTemplate
    ) {
        this.jdbcTemplate = jdbcTemplate;
    }


    public void run(ApplicationArguments args) {

        log.info("Starte einmalige Migration der Allergen-Spalten.");

        int updatedRows = jdbcTemplate.update("""
                UPDATE brotbestand
                SET
                    allergen_eier = COALESCE(allergen_eier, FALSE),
                    allergen_milch = COALESCE(allergen_milch, FALSE),
                    allergen_sesam = COALESCE(allergen_sesam, FALSE),
                    allergen_schalenfruechte = COALESCE(allergen_schalenfruechte, FALSE),
                    allergen_sellerie = COALESCE(allergen_sellerie, FALSE),
                    allergen_soja = COALESCE(allergen_soja, FALSE)
                """);

        log.info(
                "Allergen-Datenbankmigration abgeschlossen. {} Zeilen aktualisiert.",
                updatedRows
        );
    }
}