package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import org.springframework.http.HttpStatus;

/**
 * Fehler der Benutzerverwaltung mit einer für das Frontend
 * lesbaren (deutschen) Meldung und dem passenden HTTP-Status.
 */
public class KeycloakAdminException extends RuntimeException {

    private final HttpStatus status;

    public KeycloakAdminException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public KeycloakAdminException(
            HttpStatus status,
            String message,
            Throwable cause
    ) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
