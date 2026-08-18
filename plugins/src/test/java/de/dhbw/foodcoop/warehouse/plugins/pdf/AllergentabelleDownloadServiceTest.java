package de.dhbw.foodcoop.warehouse.plugins.pdf;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AllergentabelleDownloadServiceTest {

    @TempDir
    Path tempDir;

    private HttpServer server;

    private AllergentabelleDownloadService service;

    private String remoteVersion;

    private String customPageHtml;

    private byte[] pdfContent;

    private AtomicInteger pdfDownloadCount;

    @BeforeEach
    void setUp() throws IOException {

        remoteVersion = "2026-05";

        pdfContent = (
                "%PDF-1.7\n"
                        + "fake test pdf"
        ).getBytes(StandardCharsets.UTF_8);

        pdfDownloadCount = new AtomicInteger();

        server = HttpServer.create(
                new InetSocketAddress("localhost", 0),
                0
        );

        server.createContext(
                "/allergentabelle/",
                this::handleAllergentabellePage
        );

        server.createContext(
                "/wp-content/uploads/pdf/",
                this::handlePdfDownload
        );

        server.start();

        URI pageUri = URI.create(
                "http://localhost:"
                        + server.getAddress().getPort()
                        + "/allergentabelle/"
        );

        service = new AllergentabelleDownloadService(
                HttpClient.newHttpClient(),
                pageUri
        );
    }

    @AfterEach
    void tearDown() {

        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void shouldDownloadPdfWhenNoLocalVersionExists()
            throws Exception {

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isPresent());

        Path downloadedFile = result.get();

        assertEquals(
                "Allergentabelle-2026-05.pdf",
                downloadedFile.getFileName().toString()
        );

        assertTrue(Files.exists(downloadedFile));

        assertArrayEquals(
                pdfContent,
                Files.readAllBytes(downloadedFile)
        );

        assertEquals(
                1,
                pdfDownloadCount.get()
        );
    }

    @Test
    void shouldNotDownloadWhenSameVersionAlreadyExists()
            throws Exception {

        Files.writeString(
                tempDir.resolve(
                        "Allergentabelle-2026-05.pdf"
                ),
                "already downloaded"
        );

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isEmpty());

        /*
         * Wichtig:
         * Der PDF-Endpunkt wurde überhaupt nicht aufgerufen.
         */
        assertEquals(
                0,
                pdfDownloadCount.get()
        );
    }

    @Test
    void shouldDownloadWhenRemoteVersionIsNewer()
            throws Exception {

        Files.writeString(
                tempDir.resolve(
                        "Allergentabelle-2026-04.pdf"
                ),
                "old pdf"
        );

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isPresent());

        assertEquals(
                "Allergentabelle-2026-05.pdf",
                result.get()
                        .getFileName()
                        .toString()
        );

        assertEquals(
                1,
                pdfDownloadCount.get()
        );

        /*
         * Die alte Datei bleibt bestehen.
         */
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "Allergentabelle-2026-04.pdf"
                        )
                )
        );

        /*
         * Neue Datei wurde hinzugefügt.
         */
        assertTrue(
                Files.exists(
                        tempDir.resolve(
                                "Allergentabelle-2026-05.pdf"
                        )
                )
        );
    }

    @Test
    void shouldNotDownloadWhenLocalVersionIsNewer()
            throws Exception {

        Files.writeString(
                tempDir.resolve(
                        "Allergentabelle-2026-06.pdf"
                ),
                "future/newer pdf"
        );

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isEmpty());

        assertEquals(
                0,
                pdfDownloadCount.get()
        );
    }

    @Test
    void shouldIgnoreUnrelatedPdfFiles()
            throws Exception {

        Files.writeString(
                tempDir.resolve("rechnung.pdf"),
                "some other pdf"
        );

        Files.writeString(
                tempDir.resolve("speisekarte.pdf"),
                "some other pdf"
        );

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isPresent());

        assertEquals(
                "Allergentabelle-2026-05.pdf",
                result.get()
                        .getFileName()
                        .toString()
        );
    }

    @Test
    void shouldThrowExceptionWhenNoAllergentabelleLinkExists() {

        customPageHtml = """
                <html>
                    <body>
                        <a href="/some-other-file.pdf">
                            Andere PDF
                        </a>
                    </body>
                </html>
                """;

        IOException exception = assertThrows(
                IOException.class,
                () -> service.downloadIfNewer(tempDir)
        );

        assertTrue(
                exception.getMessage()
                        .contains("Keine Allergentabelle-PDF")
        );

        assertEquals(
                0,
                pdfDownloadCount.get()
        );
    }

    @Test
    void shouldHandleFutureVersionAutomatically()
            throws Exception {

        remoteVersion = "2027-02";

        Files.writeString(
                tempDir.resolve(
                        "Allergentabelle-2026-12.pdf"
                ),
                "old"
        );

        Optional<Path> result =
                service.downloadIfNewer(tempDir);

        assertTrue(result.isPresent());

        assertEquals(
                "Allergentabelle-2027-02.pdf",
                result.get()
                        .getFileName()
                        .toString()
        );
    }

    private void handleAllergentabellePage(
            HttpExchange exchange
    ) throws IOException {

        String html;

        if (customPageHtml != null) {

            html = customPageHtml;

        } else {

            html = """
                    <!DOCTYPE html>
                    <html>
                    <body>
                        <div class="yellow-box">
                            <p>
                                <a
                                    class="mtli_attachment mtli_pdf"
                                    href="/wp-content/uploads/pdf/Allergentabelle-%s.pdf"
                                    target="_blank"
                                    rel="noopener noreferrer">
                                    Allergentabelle
                                </a>
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(remoteVersion);
        }

        respond(
                exchange,
                200,
                "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8)
        );
    }

    private void handlePdfDownload(
            HttpExchange exchange
    ) throws IOException {

        pdfDownloadCount.incrementAndGet();

        String expectedPath =
                "/wp-content/uploads/pdf/Allergentabelle-"
                        + remoteVersion
                        + ".pdf";

        if (!exchange.getRequestURI()
                .getPath()
                .equals(expectedPath)) {

            respond(
                    exchange,
                    404,
                    "text/plain",
                    "Not found".getBytes(
                            StandardCharsets.UTF_8
                    )
            );

            return;
        }

        respond(
                exchange,
                200,
                "application/pdf",
                pdfContent
        );
    }

    private void respond(
            HttpExchange exchange,
            int statusCode,
            String contentType,
            byte[] body
    ) throws IOException {

        exchange.getResponseHeaders()
                .set(
                        "Content-Type",
                        contentType
                );

        exchange.sendResponseHeaders(
                statusCode,
                body.length
        );

        try (var output =
                     exchange.getResponseBody()) {

            output.write(body);
        }
    }
}