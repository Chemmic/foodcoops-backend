package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Client;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Credential;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.ErrorResponse;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Role;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.TokenResponse;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

/**
 * ============================================================================
 * Keycloak Admin REST API
 * ============================================================================
 *
 * Das Backend meldet sich per Client-Credentials-Flow mit dem vertraulichen
 * Client "foodcoop-backend" (Service Account) bei Keycloak an und ruft damit
 * die Admin-API des Realms auf.
 *
 * Der Service Account benötigt im Client "realm-management" die Rollen:
 *
 *   view-users, query-users, manage-users,
 *   view-clients, query-clients    (Client-Rollen lesen / zuweisen)
 *   manage-clients                 (Client-Rollen anlegen / löschen)
 *
 * Bei Realm-Rollen (role-client-id leer) statt *-clients: view-realm, manage-realm.
 *
 * Das Client Secret kommt ausschließlich aus der Umgebungsvariable
 * KEYCLOAK_BACKEND_CLIENT_SECRET und gelangt nie ins Frontend.
 *
 * ============================================================================
 */
@Component
public class KeycloakAdminClient {

    private static final Logger LOG =
            LoggerFactory.getLogger(KeycloakAdminClient.class);

    /** Obergrenze für Listen – eine Food-Coop hat deutlich weniger Mitglieder. */
    private static final int MAX_RESULTS = 2000;

    private static final ParameterizedTypeReference<List<User>> USER_LIST =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Role>> ROLE_LIST =
            new ParameterizedTypeReference<>() {
            };

    private static final ParameterizedTypeReference<List<Client>> CLIENT_LIST =
            new ParameterizedTypeReference<>() {
            };


    private final String serverUrl;
    private final String realm;
    private final String clientId;
    private final String clientSecret;
    private final String roleClientId;

    private final RestClient restClient;

    private String roleClientUuid;

    private String accessToken;
    private Instant accessTokenValidUntil = Instant.EPOCH;


    public KeycloakAdminClient(
            @Value("${foodcoops.keycloak.server-url:http://localhost:8089}")
            String serverUrl,

            @Value("${foodcoops.keycloak.realm:foodcoop}")
            String realm,

            @Value("${foodcoops.keycloak.backend-client-id:foodcoop-backend}")
            String clientId,

            @Value("${foodcoops.keycloak.backend-client-secret:}")
            String clientSecret,

            @Value("${foodcoops.keycloak.role-client-id:foodcoop-pwa}")
            String roleClientId
    ) {
        this.serverUrl = serverUrl.replaceAll("/+$", "");
        this.realm = realm;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.roleClientId = roleClientId == null ? "" : roleClientId.trim();

        JdkClientHttpRequestFactory requestFactory =
                new JdkClientHttpRequestFactory(
                        HttpClient.newBuilder()
                                .connectTimeout(Duration.ofSeconds(5))
                                .build()
                );

        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        this.restClient =
                RestClient.builder()
                        .requestFactory(requestFactory)
                        .build();
    }


    // =========================================================================
    // Benutzer
    // =========================================================================

    public List<User> listUsers() {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri(
                                "/users?briefRepresentation=false&first=0&max="
                                        + MAX_RESULTS
                        ))
                        .retrieve()
                        .body(USER_LIST)
        );
    }


    public User getUser(String userId) {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri("/users/{id}", userId))
                        .retrieve()
                        .body(User.class)
        );
    }


    /**
     * @return ID des neu angelegten Benutzers
     */
    public String createUser(User user) {
        URI location = call(() ->
                admin()
                        .post()
                        .uri(adminUri("/users"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user)
                        .retrieve()
                        .toBodilessEntity()
                        .getHeaders()
                        .getLocation()
        );

        if (location == null) {
            throw new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak hat keine ID für den neuen Benutzer geliefert."
            );
        }

        String path = location.getPath();

        return path.substring(path.lastIndexOf('/') + 1);
    }


    public void updateUser(String userId, User user) {
        call(() ->
                admin()
                        .put()
                        .uri(adminUri("/users/{id}", userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(user)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public void deleteUser(String userId) {
        call(() ->
                admin()
                        .delete()
                        .uri(adminUri("/users/{id}", userId))
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public void resetPassword(String userId, Credential credential) {
        call(() ->
                admin()
                        .put()
                        .uri(adminUri("/users/{id}/reset-password", userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(credential)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    /**
     * Keycloak verschickt eine E-Mail mit einem Link, über den der Benutzer
     * die angegebenen Aktionen (z.B. UPDATE_PASSWORD) ausführt.
     *
     * Setzt voraus, dass im Realm ein SMTP-Server eingerichtet ist.
     */
    public void executeActionsEmail(
            String userId,
            List<String> actions,
            String redirectClientId,
            String redirectUri
    ) {
        StringBuilder path =
                new StringBuilder("/users/{id}/execute-actions-email");

        boolean withRedirect =
                StringUtils.hasText(redirectClientId)
                        && StringUtils.hasText(redirectUri);

        if (withRedirect) {
            path.append("?client_id={clientId}&redirect_uri={redirectUri}");
        }

        URI uri = withRedirect
                ? adminUri(path.toString(), userId, redirectClientId, redirectUri)
                : adminUri(path.toString(), userId);

        call(() ->
                admin()
                        .put()
                        .uri(uri)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(actions)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public void logout(String userId) {
        call(() ->
                admin()
                        .post()
                        .uri(adminUri("/users/{id}/logout", userId))
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    // =========================================================================
    // Rollen-Container
    // =========================================================================
    //
    // role-client-id gesetzt (Standard: foodcoop-pwa):
    //     Client-Rollen dieses Clients  -> /clients/{uuid}/roles
    // role-client-id leer:
    //     Realm-Rollen                  -> /roles
    //
    // =========================================================================

    private String rolesPath() {
        return usesClientRoles()
                ? "/clients/" + roleClientUuid() + "/roles"
                : "/roles";
    }


    private String userRoleMappingPath() {
        return usesClientRoles()
                ? "/users/{id}/role-mappings/clients/" + roleClientUuid()
                : "/users/{id}/role-mappings/realm";
    }


    public boolean usesClientRoles() {
        return StringUtils.hasText(roleClientId);
    }


    /**
     * Interne ID (UUID) des Clients, dessen Rollen verwaltet werden.
     */
    private synchronized String roleClientUuid() {
        if (roleClientUuid != null) {
            return roleClientUuid;
        }

        List<Client> clients = call(() ->
                admin()
                        .get()
                        .uri(adminUri("/clients?clientId={clientId}", roleClientId))
                        .retrieve()
                        .body(CLIENT_LIST)
        );

        roleClientUuid = clients == null
                ? null
                : clients.stream()
                        .filter(client -> roleClientId.equals(client.clientId()))
                        .map(Client::id)
                        .findFirst()
                        .orElse(null);

        if (roleClientUuid == null) {
            throw new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Der Keycloak-Client \"" + roleClientId
                            + "\" für die Rollen wurde nicht gefunden."
            );
        }

        return roleClientUuid;
    }


    // =========================================================================
    // Rollen eines Benutzers (direkt zugewiesen)
    // =========================================================================

    public List<Role> getUserRoles(String userId) {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri(userRoleMappingPath(), userId))
                        .retrieve()
                        .body(ROLE_LIST)
        );
    }


    public void addUserRoles(String userId, List<Role> roles) {
        if (roles.isEmpty()) {
            return;
        }

        call(() ->
                admin()
                        .post()
                        .uri(adminUri(userRoleMappingPath(), userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(roles)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public void removeUserRoles(String userId, List<Role> roles) {
        if (roles.isEmpty()) {
            return;
        }

        call(() ->
                admin()
                        .method(HttpMethod.DELETE)
                        .uri(adminUri(userRoleMappingPath(), userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(roles)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    // =========================================================================
    // Rollen
    // =========================================================================

    public List<Role> listRoles() {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri(rolesPath() + "?briefRepresentation=false"))
                        .retrieve()
                        .body(ROLE_LIST)
        );
    }


    /**
     * Benutzer, denen die Rolle direkt zugewiesen ist.
     */
    public List<User> getRoleUsers(String roleName) {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri(
                                rolesPath()
                                        + "/{name}/users?briefRepresentation=false&first=0&max="
                                        + MAX_RESULTS,
                                roleName
                        ))
                        .retrieve()
                        .body(USER_LIST)
        );
    }


    // =========================================================================
    // Realm-Rollen (unabhängig von role-client-id)
    // =========================================================================
    //
    // Für die Admin-Rolle: Sie kann auch als Realm-Rolle angelegt sein,
    // während die übrigen Rollen Client-Rollen sind.
    //
    // =========================================================================

    /** Realm-Rolle oder null, wenn es sie nicht gibt. */
    public Role findRealmRole(String roleName) {
        try {
            return call(() ->
                    admin()
                            .get()
                            .uri(adminUri("/roles/{name}", roleName))
                            .retrieve()
                            .body(Role.class)
            );
        } catch (KeycloakAdminException exception) {
            if (exception.getStatus() == HttpStatus.NOT_FOUND) {
                return null;
            }

            throw exception;
        }
    }


    public List<Role> getUserRealmRoles(String userId) {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri("/users/{id}/role-mappings/realm", userId))
                        .retrieve()
                        .body(ROLE_LIST)
        );
    }


    public void addUserRealmRoles(String userId, List<Role> roles) {
        if (roles.isEmpty()) {
            return;
        }

        call(() ->
                admin()
                        .post()
                        .uri(adminUri("/users/{id}/role-mappings/realm", userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(roles)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public void removeUserRealmRoles(String userId, List<Role> roles) {
        if (roles.isEmpty()) {
            return;
        }

        call(() ->
                admin()
                        .method(HttpMethod.DELETE)
                        .uri(adminUri("/users/{id}/role-mappings/realm", userId))
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(roles)
                        .retrieve()
                        .toBodilessEntity()
        );
    }


    public List<User> getRealmRoleUsers(String roleName) {
        return call(() ->
                admin()
                        .get()
                        .uri(adminUri(
                                "/roles/{name}/users?briefRepresentation=false&first=0&max="
                                        + MAX_RESULTS,
                                roleName
                        ))
                        .retrieve()
                        .body(USER_LIST)
        );
    }


    // =========================================================================
    // Intern
    // =========================================================================

    /**
     * Baut eine URI der Admin-API. Pfad- und Query-Variablen werden
     * vollständig kodiert (wichtig z.B. für Rollennamen mit Umlauten).
     */
    private URI adminUri(String path, Object... variables) {
        return UriComponentsBuilder
                .fromUriString(serverUrl + "/admin/realms/{realm}" + path)
                .encode()
                .buildAndExpand(prepend(realm, variables))
                .toUri();
    }


    private static Object[] prepend(Object first, Object[] rest) {
        Object[] all = new Object[rest.length + 1];
        all[0] = first;
        System.arraycopy(rest, 0, all, 1, rest.length);
        return all;
    }


    private RestClient admin() {
        String token = accessToken();

        return restClient
                .mutate()
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build();
    }


    /**
     * Holt (und cached) ein Access Token für den Service Account.
     */
    private synchronized String accessToken() {
        if (!StringUtils.hasText(clientSecret)) {
            throw new KeycloakAdminException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Die Benutzerverwaltung ist nicht eingerichtet: "
                            + "KEYCLOAK_BACKEND_CLIENT_SECRET fehlt im Backend."
            );
        }

        if (accessToken != null
                && Instant.now().isBefore(accessTokenValidUntil)) {
            return accessToken;
        }

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);

        TokenResponse response;

        try {
            response =
                    restClient
                            .post()
                            .uri(
                                    serverUrl
                                            + "/realms/{realm}/protocol/openid-connect/token",
                                    realm
                            )
                            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                            .body(form)
                            .retrieve()
                            .body(TokenResponse.class);
        } catch (RestClientResponseException exception) {
            LOG.error(
                    "Keycloak-Anmeldung des Backends fehlgeschlagen: {} {}",
                    exception.getStatusCode(),
                    exception.getResponseBodyAsString()
            );

            throw new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Das Backend konnte sich nicht bei Keycloak anmelden. "
                            + "Client-ID / Client-Secret und Service Account prüfen.",
                    exception
            );
        } catch (ResourceAccessException exception) {
            throw unreachable(exception);
        }

        if (response == null || response.accessToken() == null) {
            throw new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak hat kein Access Token geliefert."
            );
        }

        accessToken = response.accessToken();

        accessTokenValidUntil =
                Instant.now().plusSeconds(
                        Math.max(0, response.expiresIn() - 30)
                );

        return accessToken;
    }


    private <T> T call(Supplier<T> request) {
        try {
            return request.get();
        } catch (RestClientResponseException exception) {
            throw translate(exception);
        } catch (ResourceAccessException exception) {
            throw unreachable(exception);
        }
    }


    private KeycloakAdminException translate(
            RestClientResponseException exception
    ) {
        int status = exception.getStatusCode().value();

        String keycloakMessage = null;

        try {
            ErrorResponse error =
                    exception.getResponseBodyAs(ErrorResponse.class);

            if (error != null) {
                keycloakMessage = error.message();
            }
        } catch (RuntimeException ignored) {
            // Kein JSON – dann bleibt es bei der Standardmeldung.
        }

        if (status == 401) {
            // Token evtl. serverseitig widerrufen – beim nächsten Mal neu holen.
            synchronized (this) {
                accessToken = null;
            }
        }

        // 404 ist oft erwartbar (z.B. Rolle gibt es nicht) – der Aufrufer entscheidet
        if (status == 404) {
            LOG.debug(
                    "Keycloak Admin API: {} {}",
                    status,
                    exception.getResponseBodyAsString()
            );
        } else {
            LOG.warn(
                    "Keycloak Admin API: {} {}",
                    status,
                    exception.getResponseBodyAsString()
            );
        }

        if (keycloakMessage != null
                && keycloakMessage.toLowerCase().contains("email")
                && status >= 500) {
            return new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Keycloak konnte die E-Mail nicht senden. Bitte in Keycloak unter "
                            + "Realm settings → Email Absender und SMTP-Server eintragen.",
                    exception
            );
        }

        return switch (status) {
            case 401, 403 -> new KeycloakAdminException(
                    HttpStatus.BAD_GATEWAY,
                    "Das Backend hat keine Berechtigung für die Keycloak-Admin-API. "
                            + "Bitte die Service-Account-Rollen des Clients prüfen.",
                    exception
            );

            case 404 -> new KeycloakAdminException(
                    HttpStatus.NOT_FOUND,
                    "Benutzer oder Rolle wurde in Keycloak nicht gefunden.",
                    exception
            );

            case 409 -> new KeycloakAdminException(
                    HttpStatus.CONFLICT,
                    keycloakMessage != null
                            && keycloakMessage.toLowerCase().contains("role")
                            ? "Eine Rolle mit diesem Namen existiert bereits."
                            : "Benutzername oder E-Mail-Adresse ist bereits vergeben.",
                    exception
            );

            default -> status >= 400 && status < 500
                    ? new KeycloakAdminException(
                            HttpStatus.BAD_REQUEST,
                            keycloakMessage != null
                                    ? "Keycloak: " + keycloakMessage
                                    : "Keycloak hat die Anfrage abgelehnt.",
                            exception
                    )
                    : new KeycloakAdminException(
                            HttpStatus.BAD_GATEWAY,
                            "Keycloak hat einen Fehler gemeldet (" + status + ").",
                            exception
                    );
        };
    }


    private static KeycloakAdminException unreachable(Exception exception) {
        return new KeycloakAdminException(
                HttpStatus.BAD_GATEWAY,
                "Keycloak ist nicht erreichbar.",
                exception
        );
    }
}
