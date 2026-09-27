package de.dhbw.foodcoop.warehouse.plugins.email;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * ============================================================================
 * Name der Foodcoop und Betreffzeilen der E-Mails (aus der .env)
 * ============================================================================
 *
 *   FOODCOOP_NAME                    z.B. "FoodCoop MiKa"
 *   MAIL_BETREFF_EINKAUF             Bestätigung an die Person, die einkauft
 *   MAIL_BETREFF_EINKAUFSMANAGEMENT  Info an das Einkaufsmanagement
 *
 * Platzhalter in den Betreffzeilen: %foodcoop%, %datum%, %person%
 *
 * ============================================================================
 */
@Component
public class MailTexte {

    public static final String PLATZHALTER_FOODCOOP = "%foodcoop%";
    public static final String PLATZHALTER_DATUM = "%datum%";
    public static final String PLATZHALTER_PERSON = "%person%";

    private static final DateTimeFormatter DATUM =
            DateTimeFormatter.ofPattern("dd.MM.yyyy");


    private final String name;
    private final String betreffEinkauf;
    private final String betreffEinkaufsmanagement;


    public MailTexte(
            @Value("${foodcoops.name:FoodCoop MiKa}")
            String name,

            @Value("${foodcoops.mail.betreff.einkauf:Dein Einkauf bei der %foodcoop% am %datum%}")
            String betreffEinkauf,

            @Value("${foodcoops.mail.betreff.einkaufsmanagement:Einkauf von %person% bei der %foodcoop% am %datum%}")
            String betreffEinkaufsmanagement
    ) {
        this.name = oder(name, "FoodCoop");
        this.betreffEinkauf = oder(betreffEinkauf, "Dein Einkauf bei der %foodcoop% am %datum%");
        this.betreffEinkaufsmanagement = oder(betreffEinkaufsmanagement, "Einkauf von %person% bei der %foodcoop% am %datum%");
    }


    public String name() {
        return name;
    }


    /** Betreff der Bestätigung an die Person, die eingekauft hat. */
    public String betreffEinkauf(String person, LocalDate datum) {
        return ersetzen(betreffEinkauf, person, datum);
    }


    /** Betreff der Info an das Einkaufsmanagement. */
    public String betreffEinkaufsmanagement(String person, LocalDate datum) {
        return ersetzen(betreffEinkaufsmanagement, person, datum);
    }


    /** Betreff für Listen und Übersichten, z.B. "FoodCoop MiKa – Bestellübersicht vom 27.09.2026". */
    public String betreff(String thema) {
        return name + " – " + thema;
    }


    /** Grußformel am Ende einer Mail. */
    public String gruss() {
        return "Viele Grüße\nDeine " + name;
    }


    /** Datum wie in den Mails. */
    public static String datum(LocalDate datum) {
        return datum.format(DATUM);
    }


    private String ersetzen(String vorlage, String person, LocalDate datum) {
        return vorlage
                .replace(PLATZHALTER_FOODCOOP, name)
                .replace(PLATZHALTER_DATUM, datum(datum))
                .replace(PLATZHALTER_PERSON, person == null ? "" : person)
                .trim();
    }


    private static String oder(String wert, String ersatz) {
        return wert == null || wert.isBlank()
                ? ersatz
                : wert.trim();
    }
}
