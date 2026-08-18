package de.dhbw.foodcoop.warehouse.plugins.pdf;

import de.dhbw.foodcoop.warehouse.application.allergen.AllergentabelleProvider;
import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;

@Component
public class FasanenbrotAllergentabelleProvider
        implements AllergentabelleProvider {

    private static final Logger log =
            LoggerFactory.getLogger(FasanenbrotAllergentabelleProvider.class);

    private static final Path DOWNLOAD_DIRECTORY =
            Path.of("data", "allergentabellen");

    private final AllergentabelleDownloadService downloadService;
    private final PdfAllergentabelleImporter importer;

    public FasanenbrotAllergentabelleProvider(
            AllergentabelleDownloadService downloadService,
            PdfAllergentabelleImporter importer
    ) {
        this.downloadService = downloadService;
        this.importer = importer;
    }

    @Override
    public Optional<Map<String, AllergenInfo>> loadIfUpdated() {

        try {
            Optional<Path> downloaded =
                    downloadService.downloadIfNewer(
                            DOWNLOAD_DIRECTORY
                    );

            if (downloaded.isEmpty()) {
                log.info("Keine neue Allergentabelle vorhanden.");
                return Optional.empty();
            }

            Path pdf = downloaded.get();

            log.info(
                    "Neue Allergentabelle heruntergeladen: {}",
                    pdf.toAbsolutePath()
            );

            Map<String, AllergenInfo> result =
                    importer.read(pdf.toFile());

            log.info(
                    "Allergentabelle erfolgreich eingelesen: {} Artikel gefunden.",
                    result.size()
            );

            return Optional.of(result);

        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Allergentabelle konnte nicht aktualisiert werden.",
                    exception
            );
        }
    }
}