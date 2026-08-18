package de.dhbw.foodcoop.warehouse.plugins.pdf;

import de.dhbw.foodcoop.warehouse.application.allergen.AllergenUpdateService;
import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@Order(10)
public class AllergentabelleUpdateScheduler
        implements ApplicationRunner {

    private static final Logger log =
            LoggerFactory.getLogger(
                    AllergentabelleUpdateScheduler.class
            );

    private final AllergenUpdateService allergenUpdateService;

    public AllergentabelleUpdateScheduler(
            AllergenUpdateService allergenUpdateService
    ) {
        this.allergenUpdateService =
                allergenUpdateService;
    }

    /**
     * Einmal beim Start.
     */
    @Override
    public void run(ApplicationArguments args) {
        log.info(
                "Prüfe beim Anwendungsstart auf eine neue Allergentabelle."
        );

        update();
    }

    /**
     * Danach alle 24 Stunden.
     */
    @Scheduled(
            fixedDelay = 24,
            initialDelay = 24,
            timeUnit = TimeUnit.HOURS
    )
    public void scheduledCheck() {
        log.info(
                "Starte regelmäßige Prüfung der Allergentabelle."
        );

        update();
    }

    private void update() {
        try {
            allergenUpdateService.updateIfNecessary();

        } catch (Exception exception) {
            log.error(
                    "Fehler beim Aktualisieren der Allergendaten.",
                    exception
            );
        }
    }
}