package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import java.util.List;

/**
 * Datenobjekte der Benutzerverwaltung zwischen Frontend und Backend.
 */
public final class BenutzerverwaltungDtos {

    private BenutzerverwaltungDtos() {
    }


    public record Benutzer(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean emailVerified,
            Long createdTimestamp,
            List<String> requiredActions,
            List<String> roles
    ) {
    }


    /**
     * Kurzform für Empfängerlisten (z.B. Mail an das Einkaufsmanagement).
     */
    public record BenutzerKontakt(
            String id,
            String username,
            String email,
            String firstName,
            String lastName
    ) {
    }


    public record BenutzerAnlegen(
            String username,
            String email,
            String firstName,
            String lastName,
            Boolean enabled,
            List<String> roles,

            /* Optional: Startpasswort */
            String password,
            Boolean temporaryPassword,

            /* Optional: Keycloak verschickt eine Einrichtungs-E-Mail */
            Boolean sendSetupEmail
    ) {
    }


    public record BenutzerAendern(
            String email,
            String firstName,
            String lastName,
            Boolean enabled
    ) {
    }


    public record RollenSetzen(
            List<String> roles
    ) {
    }


    public record PasswortSetzen(
            String password,
            Boolean temporary
    ) {
    }


    public record AktionsEmail(
            List<String> actions
    ) {
    }


    public record Rolle(
            String name,
            String description,
            int userCount,
            boolean protectedRole
    ) {
    }


    public record Fehler(
            String message
    ) {
    }
}
