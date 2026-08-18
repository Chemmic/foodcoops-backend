package de.dhbw.foodcoop.warehouse.plugins.pdf;

import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;
import de.dhbw.foodcoop.warehouse.domain.values.GetreideTyp;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PdfAllergentabelleImporterTest {

    private final PdfAllergentabelleImporter importer =
            new PdfAllergentabelleImporter();

    @Test
    void shouldReadProductsFromPdf() throws Exception {

        File pdf = getTestPdf();

        Map<String, AllergenInfo> result = importer.read(pdf);

        assertNotNull(result);
        assertFalse(result.isEmpty());

        assertTrue(
                result.containsKey("Saftbrot ESSENER Art"),
                "Saftbrot ESSENER Art sollte in der PDF gefunden werden"
        );
        assertEquals(100, result.size());
    }

    @Test
    void shouldReadGrainTypesCorrectly() throws Exception {

        File pdf = getTestPdf();

        Map<String, AllergenInfo> result = importer.read(pdf);

        AllergenInfo info = result.get("Saftbrot ESSENER Art");

        assertNotNull(info);

        assertTrue(info.getGetreide().contains(GetreideTyp.WEIZEN));
        assertTrue(info.getGetreide().contains(GetreideTyp.ROGGEN));
        assertTrue(info.getGetreide().contains(GetreideTyp.HAFER));
    }

    @Test
    void shouldReadAllergensCorrectly() throws Exception {

        File pdf = getTestPdf();

        Map<String, AllergenInfo> result = importer.read(pdf);

        AllergenInfo info = result.get("Saftbrot ESSENER Art");

        assertNotNull(info);

        assertFalse(info.isEier());
        assertTrue(info.isMilch());
        assertTrue(info.isSesam());
        assertFalse(info.isSchalenfruechte());
        assertTrue(info.isSellerie());
        assertFalse(info.isSoja());
    }

    @Test
    void shouldHaveSoja() throws Exception {
        File pdf = getTestPdf();

        Map<String, AllergenInfo> result = importer.read(pdf);

        AllergenInfo info = result.get("Schoko Croissant");

        assertNotNull(info);
        assertTrue(info.isSoja());
        assertAll(
                result.entrySet().stream()
                        .filter(entry -> !entry.getKey().equals("Schoko Croissant"))
                        .map(entry -> () ->
                                assertFalse(
                                        entry.getValue().isSoja(),
                                        entry.getKey() + " sollte kein Soja enthalten"
                                )
                        )
        );

    }

    private File getTestPdf() throws URISyntaxException {
        var resource = getClass()
                .getClassLoader()
                .getResource("AllergenTabelle-2026-05.pdf");

        assertNotNull(
                resource,
                "Test-PDF wurde unter src/test/resources nicht gefunden"
        );

        return new File(resource.toURI());
    }

    @Test
    void shouldReadSchokoPeanutsCorrectly() throws Exception {
        File pdf = getTestPdf();

        Map<String, AllergenInfo> result = importer.read(pdf);

        AllergenInfo info = result.get("Schoko-Peanuts");

        assertNotNull(info);

        assertAll(
                () -> assertFalse(info.isEier()),
                () -> assertTrue(info.isMilch()),
                () -> assertFalse(info.isSesam()),
                () -> assertFalse(info.isSchalenfruechte()),
                () -> assertFalse(info.isSellerie()),
                () -> assertFalse(info.isSoja()),
                () -> assertEquals("Erdnuss", info.getHinweis())
        );
    }
}