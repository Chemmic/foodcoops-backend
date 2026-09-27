package de.dhbw.foodcoop.warehouse.plugins.historie;

import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakAdminClient;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

/**
 * Keycloak-Benutzer nach Benutzername (= personId der Bestellungen).
 * Optional: ist Keycloak nicht erreichbar, bleibt die Map leer und die
 * Übersichten zeigen nur die Benutzernamen.
 */
final class MitgliederNamen {

    private static final Logger LOG =
            LoggerFactory.getLogger(MitgliederNamen.class);


    private MitgliederNamen() {
    }


    static Map<String, User> laden(KeycloakAdminClient keycloak) {
        try {
            return keycloak.listUsers()
                    .stream()
                    .filter(u -> u.username() != null && !u.username().startsWith("service-account-"))
                    .collect(Collectors.toMap(User::username, Function.identity(), (a, b) -> a));
        } catch (RuntimeException exception) {
            LOG.info("Mitgliederliste ohne Keycloak-Namen: {}", exception.getMessage());
            return Map.of();
        }
    }


    /** "Vorname Nachname" oder null, wenn Keycloak keinen Namen kennt. */
    static String anzeigename(User user) {
        if (user == null) {
            return null;
        }

        String name = Stream.of(user.firstName(), user.lastName())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "))
                .trim();

        return name.isEmpty() ? null : name;
    }
}
