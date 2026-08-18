package de.dhbw.foodcoop.warehouse.plugins.pdf;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DateTimeException;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AllergentabelleDownloadService {

    private static final URI DEFAULT_ALLERGEN_PAGE =
            URI.create("https://fasanenbrot.de/allergentabelle/");

    /*
     * Beispiel:
     *
     * Allergentabelle-2026-05.pdf
     *
     * Gruppe 1 = kompletter Dateiname
     * Gruppe 2 = Jahr
     * Gruppe 3 = Monat
     */
    private static final Pattern PDF_PATTERN = Pattern.compile(
            "(Allergentabelle-(\\d{4})-(\\d{2})\\.pdf)",
            Pattern.CASE_INSENSITIVE
    );

    private final HttpClient httpClient;
    private final URI allergenPage;

    public AllergentabelleDownloadService() {
        this(
                HttpClient.newBuilder()
                        .followRedirects(HttpClient.Redirect.NORMAL)
                        .build(),
                DEFAULT_ALLERGEN_PAGE
        );
    }

    /*
     * Package-private Konstruktor für Tests.
     *
     * Dadurch können wir im Test einen lokalen HTTP-Server verwenden,
     * statt wirklich fasanenbrot.de aufzurufen.
     */
    AllergentabelleDownloadService(
            HttpClient httpClient,
            URI allergenPage
    ) {
        this.httpClient = httpClient;
        this.allergenPage = allergenPage;
    }

    /**
     * Prüft, welche Allergentabelle auf der Website aktuell verlinkt ist.
     *
     * Ist sie neuer als die neueste lokale Datei,
     * wird sie heruntergeladen.
     *
     * Gibt Optional.empty() zurück, wenn lokal bereits
     * dieselbe oder eine neuere Version vorhanden ist.
     */
    public Optional<Path> downloadIfNewer(
            Path downloadDirectory
    ) throws IOException, InterruptedException {

        Files.createDirectories(downloadDirectory);

        RemotePdf remotePdf = findRemotePdf();

        Optional<YearMonth> localVersion =
                findNewestLocalVersion(downloadDirectory);

        if (localVersion.isPresent()
                && !remotePdf.version().isAfter(localVersion.get())) {

            return Optional.empty();
        }

        Path downloadedFile =
                downloadPdf(remotePdf, downloadDirectory);

        return Optional.of(downloadedFile);
    }

    /**
     * Lädt die HTML-Seite und sucht dort nach einem Link wie
     *
     * Allergentabelle-2026-05.pdf
     */
    private RemotePdf findRemotePdf()
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(allergenPage)
                .header(
                        "User-Agent",
                        "FoodCoop-Warehouse/1.0"
                )
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofString(
                        StandardCharsets.UTF_8
                )
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Allergentabelle-Seite konnte nicht geladen werden. HTTP "
                            + response.statusCode()
            );
        }

        Document document = Jsoup.parse(
                response.body(),
                allergenPage.toString()
        );

        /*
         * Wir suchen alle Links und nehmen die Allergentabelle
         * mit der höchsten Version.
         *
         * Aktuell gibt es nur eine, aber falls irgendwann mehrere
         * im HTML stehen, funktioniert es trotzdem.
         */
        return document
                .select("a[href]")
                .stream()
                .map(this::parseRemotePdf)
                .flatMap(Optional::stream)
                .max(Comparator.comparing(RemotePdf::version))
                .orElseThrow(() ->
                        new IOException(
                                "Keine Allergentabelle-PDF auf "
                                        + allergenPage
                                        + " gefunden."
                        )
                );
    }

    private Optional<RemotePdf> parseRemotePdf(
            Element link
    ) {

        String href = link.attr("href");

        Matcher matcher = PDF_PATTERN.matcher(href);

        if (!matcher.find()) {
            return Optional.empty();
        }

        YearMonth version;

        try {
            version = YearMonth.of(
                    Integer.parseInt(matcher.group(2)),
                    Integer.parseInt(matcher.group(3))
            );
        } catch (NumberFormatException | DateTimeException exception) {
            return Optional.empty();
        }

        String absoluteUrl = link.absUrl("href");

        if (absoluteUrl.isBlank()) {
            return Optional.empty();
        }

        return Optional.of(
                new RemotePdf(
                        URI.create(absoluteUrl),
                        matcher.group(1),
                        version
                )
        );
    }

    /**
     * Sucht im lokalen Verzeichnis nach Dateien wie
     *
     * Allergentabelle-2026-04.pdf
     * Allergentabelle-2026-05.pdf
     *
     * und gibt die höchste Version zurück.
     */
    private Optional<YearMonth> findNewestLocalVersion(
            Path directory
    ) throws IOException {

        try (var files = Files.list(directory)) {

            return files
                    .filter(Files::isRegularFile)
                    .map(path ->
                            parseLocalVersion(
                                    path.getFileName().toString()
                            )
                    )
                    .flatMap(Optional::stream)
                    .max(Comparator.naturalOrder());
        }
    }

    private Optional<YearMonth> parseLocalVersion(
            String fileName
    ) {

        Matcher matcher = PDF_PATTERN.matcher(fileName);

        /*
         * Lokal soll wirklich der ganze Dateiname passen.
         */
        if (!matcher.matches()) {
            return Optional.empty();
        }

        try {
            return Optional.of(
                    YearMonth.of(
                            Integer.parseInt(matcher.group(2)),
                            Integer.parseInt(matcher.group(3))
                    )
            );
        } catch (NumberFormatException | DateTimeException exception) {
            return Optional.empty();
        }
    }

    private Path downloadPdf(
            RemotePdf remotePdf,
            Path downloadDirectory
    ) throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(remotePdf.uri())
                .header(
                        "User-Agent",
                        "FoodCoop-Warehouse/1.0"
                )
                .GET()
                .build();

        HttpResponse<byte[]> response = httpClient.send(
                request,
                HttpResponse.BodyHandlers.ofByteArray()
        );

        if (response.statusCode() != 200) {
            throw new IOException(
                    "Allergentabelle konnte nicht heruntergeladen werden. HTTP "
                            + response.statusCode()
            );
        }

        byte[] content = response.body();

        /*
         * Kleine Sicherheitsprüfung:
         *
         * PDFs beginnen normalerweise mit "%PDF-".
         *
         * Dadurch speichern wir nicht aus Versehen eine
         * HTML-Fehlerseite als .pdf.
         */
        if (!looksLikePdf(content)) {
            throw new IOException(
                    "Heruntergeladene Datei scheint keine PDF zu sein."
            );
        }

        Path destination = downloadDirectory.resolve(
                remotePdf.fileName()
        );

        Files.write(
                destination,
                content
        );

        return destination;
    }

    private boolean looksLikePdf(byte[] content) {

        if (content == null || content.length < 5) {
            return false;
        }

        return content[0] == '%'
                && content[1] == 'P'
                && content[2] == 'D'
                && content[3] == 'F'
                && content[4] == '-';
    }

    private record RemotePdf(
            URI uri,
            String fileName,
            YearMonth version
    ) {
    }
}