package de.dhbw.foodcoop.warehouse.application.allergen;

import de.dhbw.foodcoop.warehouse.application.brot.BrotBestandService;
import de.dhbw.foodcoop.warehouse.domain.entities.BrotBestand;
import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AllergenUpdateService {

    private static final Logger log =
            LoggerFactory.getLogger(AllergenUpdateService.class);

    private final AllergentabelleProvider allergentabelleProvider;
    private final BrotBestandService brotBestandService;

    public AllergenUpdateService(
            AllergentabelleProvider allergentabelleProvider,
            BrotBestandService brotBestandService
    ) {
        this.allergentabelleProvider = allergentabelleProvider;
        this.brotBestandService = brotBestandService;
    }

    public void updateIfNecessary() {

        Optional<Map<String, AllergenInfo>> update =
                allergentabelleProvider.loadIfUpdated();

        if (update.isEmpty()) {
            log.info("Kein Allergendaten-Update notwendig.");
            return;
        }

        Map<String, AllergenInfo> allergenMap =
                update.get();

        Set<String> matchedPdfProducts =
                new HashSet<>();

        int updated = 0;
        int notFound = 0;

        for (BrotBestand brot : brotBestandService.all()) {

            String name = brot.getName();

            AllergenInfo allergenInfo =
                    allergenMap.get(name);

            if (allergenInfo == null) {
                log.warn(
                        "Keine Allergendaten für Brot '{}' in der PDF gefunden.",
                        name
                );

                notFound++;
                continue;
            }

            brot.setAllergenInfo(allergenInfo);

            brotBestandService.save(brot);

            matchedPdfProducts.add(name);

            updated++;

            log.info(
                    "Allergene für '{}' aktualisiert: {}",
                    name,
                    allergenInfo
            );
        }

        /*
         * Andersherum prüfen:
         * Gibt es Produkte in der PDF, die wir gar nicht
         * im Brotbestand kennen?
         */
        allergenMap.keySet().stream()
                .filter(name ->
                        !matchedPdfProducts.contains(name)
                )
                .forEach(name ->
                        log.warn(
                                "Produkt '{}' steht in der Allergentabelle, "
                                        + "existiert aber nicht im Brotbestand.",
                                name
                        )
                );

        log.info(
                "Allergen-Update abgeschlossen: {} Brote aktualisiert, "
                        + "{} vorhandene Brote ohne Treffer.",
                updated,
                notFound
        );
    }
}