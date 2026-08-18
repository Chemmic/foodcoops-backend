package de.dhbw.foodcoop.warehouse.plugins.pdf;


import de.dhbw.foodcoop.warehouse.domain.values.AllergenInfo;
import de.dhbw.foodcoop.warehouse.domain.values.GetreideTyp;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Service;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.PageIterator;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.io.IOException;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;

@Service
public class PdfAllergentabelleImporter {

    public HashMap<String, AllergenInfo> read(File pdfFile) throws IOException {

        HashMap<String, AllergenInfo> result = new HashMap<>();

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            ObjectExtractor objectExtractor = new ObjectExtractor(document);

            SpreadsheetExtractionAlgorithm algorithm =
                    new SpreadsheetExtractionAlgorithm();

            PageIterator pages = objectExtractor.extract();

            while (pages.hasNext()) {

                Page page = pages.next();

                List<Table> tables = algorithm.extract(page);

                for (Table table : tables) {
                    parseTable(table, result);
                }
            }
        }

        return result;
    }

    private void parseTable(
            Table table,
            HashMap<String, AllergenInfo> result
    ) {

        for (List<RectangularTextContainer> row : table.getRows()) {

            if (row.size() < 2) {
                continue;
            }

            String artikel = getCell(row, 0);

            if (artikel.isBlank()) {
                continue;
            }

            /*
             * Tabellenüberschrift ignorieren.
             */
            if (artikel.equalsIgnoreCase("Artikel")) {
                continue;
            }

            String getreideCell = getCell(row, 1);

            EnumSet<GetreideTyp> getreide =
                    parseGetreide(getreideCell);

            /*
             * WICHTIG:
             *
             * Nur "x" bedeutet, dass das entsprechende Allergen gesetzt ist.
             *
             * Texte wie "Erdnuss" dürfen NICHT als true interpretiert werden.
             */
            boolean eier =
                    isX(getCell(row, 2));

            boolean milch =
                    isX(getCell(row, 3));

            boolean sesam =
                    isX(getCell(row, 4));

            boolean schalenfruechte =
                    isX(getCell(row, 5));

            boolean sellerie =
                    isX(getCell(row, 6));

            /*
             * Nur die rechte Tabelle besitzt die Soja-Spalte.
             */
            boolean soja =
                    row.size() >= 8
                            && isX(getCell(row, 7));

            /*
             * Alles, was in den Allergenspalten steht,
             * aber kein "x" ist, wird als Hinweis gespeichert.
             *
             * Dadurch wird z.B. "Erdnuss" erhalten.
             */
            String hinweis = findHinweis(row);

            AllergenInfo info = new AllergenInfo(
                    getreide,
                    eier,
                    milch,
                    sesam,
                    schalenfruechte,
                    sellerie,
                    soja,
                    hinweis
            );

            result.put(artikel, info);
        }
    }

    /**
     * Sucht in allen Allergenspalten nach Werten,
     * die weder leer noch "x" sind.
     *
     * Beispiel:
     *
     * [ "", "x", "", "Erdnuss", "" ]
     *
     * -> "Erdnuss"
     *
     * Falls Tabula einen Text über mehrere Zellen verteilt,
     * werden alle Teile eingesammelt.
     */
    private String findHinweis(
            List<RectangularTextContainer> row
    ) {

        StringBuilder hinweis = new StringBuilder();

        /*
         * Index 0 = Artikel
         * Index 1 = Getreide
         *
         * Ab Index 2 kommen die Allergenspalten.
         */
        for (int i = 2; i < row.size(); i++) {

            String value = getCell(row, i);

            if (value.isBlank()) {
                continue;
            }

            if (isX(value)) {
                continue;
            }

            if (!hinweis.isEmpty()) {
                hinweis.append(" ");
            }

            hinweis.append(value);
        }

        if (hinweis.isEmpty()) {
            return null;
        }

        return hinweis.toString().trim();
    }

    private String getCell(
            List<RectangularTextContainer> row,
            int index
    ) {

        if (index < 0 || index >= row.size()) {
            return "";
        }

        String text = row.get(index).getText();

        if (text == null) {
            return "";
        }

        return text
                .replace("\r", " ")
                .replace("\n", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * Ein Allergen gilt ausschließlich dann als markiert,
     * wenn wirklich "x" in der Zelle steht.
     */
    private boolean isX(String cell) {

        if (cell == null) {
            return false;
        }

        return cell.trim().equalsIgnoreCase("x");
    }

    private EnumSet<GetreideTyp> parseGetreide(
            String value
    ) {

        EnumSet<GetreideTyp> result =
                EnumSet.noneOf(GetreideTyp.class);

        if (value == null || value.isBlank()) {
            return result;
        }

        String normalized = value
                .toUpperCase()
                .replaceAll("\\s+", "");

        for (char c : normalized.toCharArray()) {

            GetreideTyp.fromCode(c)
                    .ifPresent(result::add);
        }

        return result;
    }
}