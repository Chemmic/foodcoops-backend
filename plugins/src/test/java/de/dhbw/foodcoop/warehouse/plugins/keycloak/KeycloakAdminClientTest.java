package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

/**
 * Testet den Client gegen einen minimalen Fake-Keycloak (JDK HttpServer).
 */
class KeycloakAdminClientTest {

    private HttpServer server;
    private String baseUrl;

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<String> authHeaders = new CopyOnWriteArrayList<>();
    private int tokenCalls;


    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);

        server.createContext("/auth/realms/foodcoop/protocol/openid-connect/token", exchange -> {
            tokenCalls++;
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

            if (!body.contains("client_secret=geheim")) {
                respond(exchange, 401, "{\"error\":\"unauthorized_client\"}");
                return;
            }

            respond(exchange, 200, "{\"access_token\":\"abc\",\"expires_in\":300}");
        });

        server.createContext("/auth/admin/realms/foodcoop/", exchange -> {
            requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getRawPath()
                    + (exchange.getRequestURI().getRawQuery() == null ? "" : "?" + exchange.getRequestURI().getRawQuery()));
            authHeaders.add(exchange.getRequestHeaders().getFirst("Authorization"));

            String path = exchange.getRequestURI().getPath();

            if (exchange.getRequestMethod().equals("POST") && path.endsWith("/users")) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

                if (body.contains("\"username\":\"doppelt\"")) {
                    respond(exchange, 409, "{\"errorMessage\":\"User exists with same username\"}");
                    return;
                }

                exchange.getResponseHeaders().add("Location", baseUrl + "/admin/realms/foodcoop/users/neue-id");
                respond(exchange, 201, "");
                return;
            }

            if (path.endsWith("/execute-actions-email")) {
                respond(exchange, 500, "{\"errorMessage\":\"Failed to send execute actions email: Invalid sender address 'null'.\"}");
                return;
            }

            if (path.endsWith("/clients")) {
                respond(exchange, 200, "[{\"id\":\"uuid-pwa\",\"clientId\":\"foodcoop-pwa\"}]");
                return;
            }

            if (path.endsWith("/foodcoop/roles/Admin")) {
                respond(exchange, 200, "{\"id\":\"r1\",\"name\":\"Admin\",\"clientRole\":false}");
                return;
            }

            if (path.endsWith("/foodcoop/roles/Fehlt")) {
                respond(exchange, 404, "{\"error\":\"Could not find role\"}");
                return;
            }

            if (path.contains("/roles/")) {
                respond(exchange, 200, "[{\"id\":\"1\",\"username\":\"anna\",\"email\":\"a@b.de\",\"unbekannt\":42}]");
                return;
            }

            respond(exchange, 404, "{\"error\":\"not found\"}");
        });

        server.start();

        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort() + "/auth";
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }


    /** Realm-Rollen */
    private KeycloakAdminClient client(String secret) {
        return new KeycloakAdminClient(baseUrl + "/", "foodcoop", "foodcoop-backend", secret, "");
    }


    @Test
    void usesClientRolesOfConfiguredClient() {
        KeycloakAdminClient client = new KeycloakAdminClient(
                baseUrl, "foodcoop", "foodcoop-backend", "geheim", "foodcoop-pwa"
        );

        client.getRoleUsers("Einkäufer");
        client.getRoleUsers("Einkäufer");

        assertThat(requests).containsExactly(
                "GET /auth/admin/realms/foodcoop/clients?clientId=foodcoop-pwa",
                "GET /auth/admin/realms/foodcoop/clients/uuid-pwa/roles/Eink%C3%A4ufer/users?briefRepresentation=false&first=0&max=2000",
                "GET /auth/admin/realms/foodcoop/clients/uuid-pwa/roles/Eink%C3%A4ufer/users?briefRepresentation=false&first=0&max=2000"
        );
    }


    @Test
    void findsRealmRoleEvenWhenClientRolesAreManaged() {
        KeycloakAdminClient client = new KeycloakAdminClient(
                baseUrl, "foodcoop", "foodcoop-backend", "geheim", "foodcoop-pwa"
        );

        assertThat(client.findRealmRole("Admin").name()).isEqualTo("Admin");
        assertThat(client.findRealmRole("Fehlt")).isNull();

        assertThat(requests).containsExactly(
                "GET /auth/admin/realms/foodcoop/roles/Admin",
                "GET /auth/admin/realms/foodcoop/roles/Fehlt"
        );
    }


    @Test
    void createsUserAndReadsIdFromLocation() {
        String id = client("geheim").createUser(new User(
                null, "clara", null, null, null, true, null, null, null, null
        ));

        assertThat(id).isEqualTo("neue-id");
        assertThat(authHeaders).containsOnly("Bearer abc");
    }

    @Test
    void cachesServiceAccountToken() {
        KeycloakAdminClient client = client("geheim");

        client.getRoleUsers("Einkaufsmanagement");
        client.getRoleUsers("Einkaufsmanagement");

        assertThat(tokenCalls).isEqualTo(1);
    }

    @Test
    void encodesRoleNamesAndIgnoresUnknownFields() {
        List<User> users = client("geheim").getRoleUsers("Einkäufer");

        assertThat(users).singleElement()
                .extracting(User::email)
                .isEqualTo("a@b.de");

        assertThat(requests).singleElement()
                .asString()
                .startsWith("GET /auth/admin/realms/foodcoop/roles/Eink%C3%A4ufer/users?");
    }

    @Test
    void explainsMissingRealmMailSetup() {
        assertThatThrownBy(() -> client("geheim").executeActionsEmail(
                "u1", List.of("UPDATE_PASSWORD"), null, null
        ))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("Realm settings → Email");
    }

    @Test
    void translatesConflict() {
        assertThatThrownBy(() -> client("geheim").createUser(new User(
                null, "doppelt", null, null, null, true, null, null, null, null
        )))
                .isInstanceOf(KeycloakAdminException.class)
                .satisfies(error -> assertThat(((KeycloakAdminException) error).getStatus())
                        .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void translatesNotFound() {
        assertThatThrownBy(() -> client("geheim").getUser("fehlt"))
                .isInstanceOf(KeycloakAdminException.class)
                .satisfies(error -> assertThat(((KeycloakAdminException) error).getStatus())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void wrongSecretGivesBadGateway() {
        assertThatThrownBy(() -> client("falsch").listUsers())
                .isInstanceOf(KeycloakAdminException.class)
                .satisfies(error -> assertThat(((KeycloakAdminException) error).getStatus())
                        .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void missingSecretGivesServiceUnavailable() {
        assertThatThrownBy(() -> client("").listUsers())
                .isInstanceOf(KeycloakAdminException.class)
                .satisfies(error -> assertThat(((KeycloakAdminException) error).getStatus())
                        .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        assertThat(tokenCalls).isZero();
    }


    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);

        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);

        if (bytes.length > 0) {
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        }

        exchange.close();
    }
}
