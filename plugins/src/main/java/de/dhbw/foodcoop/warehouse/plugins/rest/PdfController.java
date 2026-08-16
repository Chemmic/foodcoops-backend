package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;

import jakarta.mail.MessagingException;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import de.dhbw.foodcoop.warehouse.application.bestellungsliste.BestellÜbersichtService;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.plugins.email.EmailService;
import de.dhbw.foodcoop.warehouse.plugins.helpObjects.PDFInfoObject;
import de.dhbw.foodcoop.warehouse.plugins.pdf.PdfService;

@RestController
public class PdfController {

    private final EmailService service;
    private final PdfService pdf;
    private final DeadlineService deadlineService;
    private final BestellÜbersichtService bueService;

    //TODO: Es muss alles mit Keycloak abgesichert werden.
    // Wenn dies geschehen ist, kann man sich über das Authentification Objekt den Token ziehen
    // Und damit auch die Email, wenn diese drin steht im Token.
    // Dann muss keine Email im Body übergeben werden.
    public PdfController(
            EmailService service,
            PdfService pdf,
            DeadlineService deadlineService,
            BestellÜbersichtService bueService) {

        this.service = service;
        this.pdf = pdf;
        this.deadlineService = deadlineService;
        this.bueService = bueService;
    }

    @PostMapping("/email/send/bestellUebersicht")
    public void sendTotalBestellÜbersicht(@RequestBody String email) {
        String date = LocalDate.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        try {
            service.sendEmailWithPDF(
                    email,
                    "Foodcoop MIKA - Bestellübersicht vom " + date,
                    "Hallo, \nim Anhang befindet sich die Bestellübersicht vom "
                            + date
                            + ".\n\nViele Grüße \nDeine Foodcoop MIKA",
                    pdf.createUebersicht(bueService.getLastUebersicht()),
                    "Bestelluebersicht-" + date + ".pdf");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/email/send/brotBestellungen")
    public void sendBreadOrder(@RequestBody String email) {
        String date = deadlineService.getByPosition(0)
                .orElseThrow()
                .getDatum()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        try {
            service.sendEmailWithPDF(
                    email,
                    "Foodcoop MIKA - zu bestellende Brote vom " + date,
                    "Hallo, \nim Anhang befindet sich die Liste der zu bestellenden Brote für die Deadline vom "
                            + date
                            + ".\n\nViele Grüße \nDeine Foodcoop MIKA",
                    pdf.createBrotUebersicht(),
                    "Brotbestellungen-" + date + ".pdf");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/email/send/brotBestellungenMitPersonen")
    public void sendBreadOrderWithPersons(@RequestBody String email) {
        String date = deadlineService.getByPosition(0)
                .orElseThrow()
                .getDatum()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        try {
            service.sendEmailWithPDF(
                    email,
                    "Foodcoop MIKA - Brotbestellungen der Mitglieder " + date,
                    "Hallo, \nim Anhang befindet sich die Liste der einzelnen Brotbestellungen für die Deadline vom "
                            + date
                            + ".\n\nViele Grüße \nDeine Foodcoop MIKA",
                    pdf.createBrotUebersichtWithPersons(),
                    "BrotbestellungenPersonen-" + date + ".pdf");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/email/send/lagerbestand/{email}")
    public void sendInventoryStatus(
            @RequestBody String base64Pdf,
            @PathVariable String email) {

        String date = deadlineService.getByPosition(0)
                .orElseThrow()
                .getDatum()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        try {
            service.sendEmailWithPDF(
                    email,
                    "Foodcoop MIKA - aktueller Lagerbestand " + date,
                    "Hallo, \nim Anhang befindet sich der aktuelle Lagerbestand.\n\nViele Grüße \nDeine Foodcoop MIKA",
                    pdf.createByteArrayFromBase64String(base64Pdf),
                    "Lagerbestand-" + date + ".pdf");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @PostMapping("/email/send/frischBestellungen")
    public void sendFreshOrder(@RequestBody String email) {
        String date = deadlineService.getByPosition(0)
                .orElseThrow()
                .getDatum()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        try {
            service.sendEmailWithPDF(
                    email,
                    "Foodcoop MIKA - zu bestellende Frischware vom " + date,
                    "Hallo, \nim Anhang befindet sich die Liste der zu bestellenden Frischware für die Deadline vom "
                            + date
                            + ".\n\nViele Grüße \nDeine Foodcoop MIKA",
                    pdf.createFrischUebersicht(),
                    "Frischbestellungen-" + date + ".pdf");
        } catch (MessagingException e) {
            e.printStackTrace();
        }
    }

    @GetMapping("/pdf/download/frischBestellungen")
    public ResponseEntity<StreamingResponseBody> getUebersichtFrischPDF()
            throws IOException {

        String fileName = "Frischbestellungen-"
                + LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] pdfInBytes = pdf.createFrischUebersicht();

        return createPdfDownloadResponse(pdfInBytes, fileName);
    }

    @GetMapping("/pdf/download/brotBestellungen")
    public ResponseEntity<StreamingResponseBody> getUebersichtBrotPDF()
            throws IOException {

        String fileName = "Brotbestellungen-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] pdfInBytes = pdf.createBrotUebersicht();

        return createPdfDownloadResponse(pdfInBytes, fileName);
    }

    @GetMapping("/pdf/download/bestellUebersicht")
    public ResponseEntity<StreamingResponseBody> getBestellUebersichtPDF()
            throws IOException {

        String fileName = "Bestelluebersicht-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] pdfInBytes =
                pdf.createUebersicht(bueService.getLastUebersicht());

        return createPdfDownloadResponse(pdfInBytes, fileName);
    }

    @GetMapping("/pdf/download/brotBestellungenMitPerson")
    public ResponseEntity<StreamingResponseBody> getBrotBestellungenWithPersonPDF()
            throws IOException {

        String fileName = "Brotbestellungen-fuer-Personen-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        byte[] pdfInBytes = pdf.createBrotUebersichtWithPersons();

        return createPdfDownloadResponse(pdfInBytes, fileName);
    }

    @GetMapping("/pdf/byte/frischBestellungen")
    public PDFInfoObject getUebersichtFrischPDFasByte()
            throws IOException {

        String fileName = "Frischbestellungen-"
                + LocalDateTime.now()
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.TUESDAY))
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        return new PDFInfoObject(
                pdf.createFrischUebersicht(),
                fileName);
    }

    @GetMapping("/pdf/byte/brotBestellungen")
    public PDFInfoObject getUebersichtBrotPDFasByte()
            throws IOException {

        String fileName = "Brotbestellungen-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        return new PDFInfoObject(
                pdf.createBrotUebersicht(),
                fileName);
    }

    @GetMapping("/pdf/byte/bestellUebersicht")
    public PDFInfoObject getBestellUebersichtPDFasByte()
            throws IOException {

        String fileName = "Bestelluebersicht-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        return new PDFInfoObject(
                pdf.createUebersicht(bueService.getLastUebersicht()),
                fileName);
    }

    @GetMapping("/pdf/byte/brotMitPerson")
    public PDFInfoObject getBreadWithPersonPDFasByte()
            throws IOException {

        String fileName = "Brotbestellungen-fuer-Personen-"
                + LocalDateTime.now()
                .format(DateTimeFormatter.ofPattern("dd-MM-yyyy"));

        return new PDFInfoObject(
                pdf.createBrotUebersichtWithPersons(),
                fileName);
    }

    private ResponseEntity<StreamingResponseBody> createPdfDownloadResponse(
            byte[] pdfInBytes,
            String fileName) {

        StreamingResponseBody responseBody = outputStream -> {
            try (ByteArrayInputStream inputStream =
                         new ByteArrayInputStream(pdfInBytes)) {

                byte[] data = new byte[8192];
                int numberOfBytesToWrite;

                while ((numberOfBytesToWrite =
                        inputStream.read(data)) != -1) {

                    outputStream.write(
                            data,
                            0,
                            numberOfBytesToWrite);
                }
            }
        };

        return ResponseEntity.ok()
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + fileName + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(pdfInBytes.length)
                .body(responseBody);
    }
}