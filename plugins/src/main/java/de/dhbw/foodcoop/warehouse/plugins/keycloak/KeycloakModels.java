package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Ausschnitte der Keycloak-Admin-REST-API-Repräsentationen.
 *
 * Nur die Felder, die die Anwendung tatsächlich nutzt. Unbekannte Felder
 * werden ignoriert, null-Felder nicht gesendet (Keycloak lässt nicht
 * gesendete Felder beim Update unverändert).
 */
public final class KeycloakModels {

    private KeycloakModels() {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record User(
            String id,
            String username,
            String email,
            String firstName,
            String lastName,
            Boolean enabled,
            Boolean emailVerified,
            Long createdTimestamp,
            List<String> requiredActions,
            List<Credential> credentials
    ) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Role(
            String id,
            String name,
            String description,
            Boolean composite,
            Boolean clientRole
    ) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Client(
            String id,
            String clientId
    ) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Credential(
            String type,
            String value,
            Boolean temporary
    ) {

        public static Credential password(String value, boolean temporary) {
            return new Credential("password", value, temporary);
        }
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token")
            String accessToken,

            @JsonProperty("expires_in")
            long expiresIn
    ) {
    }


    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorResponse(
            String error,

            @JsonProperty("error_description")
            String errorDescription,

            String errorMessage
    ) {

        String message() {
            if (errorMessage != null && !errorMessage.isBlank()) {
                return errorMessage;
            }

            if (errorDescription != null && !errorDescription.isBlank()) {
                return errorDescription;
            }

            return error;
        }
    }
}
