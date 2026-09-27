package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Benutzer;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAendern;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAnlegen;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerKontakt;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Rolle;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Credential;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Role;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

/**
 * ============================================================================
 * Benutzerverwaltung über Keycloak
 * ============================================================================
 *
 * Verwaltet werden die Rollen des Clients foodcoops.keycloak.role-client-id
 * (Standard: foodcoop-pwa) bzw. Realm-Rollen, falls leer. Technische Keycloak-Rollen
 * (default-roles-*, offline_access, uma_authorization) werden ausgeblendet
 * und beim Setzen von Rollen nie verändert.
 *
 * Rollen werden nur vergeben, nicht angelegt – welche Rolle was darf, legt
 * die Anwendung fest (Admin, Einkäufer, Organisator, ...).
 *
 * Die Admin-Rolle ist immer vergebbar: Ist sie keine Client-Rolle, sondern
 * eine Realm-Rolle, wird sie zusätzlich angeboten und über die Realm-API
 * zugewiesen.
 *
 * Schutz vor Selbstaussperrung: Ein Admin kann sich selbst weder löschen,
 * deaktivieren noch die Admin-Rolle entziehen.
 *
 * ============================================================================
 */
@Service
public class BenutzerverwaltungService {

    private static final Logger LOG =
            LoggerFactory.getLogger(BenutzerverwaltungService.class);

    private static final Set<String> ERLAUBTE_AKTIONEN = Set.of(
            "UPDATE_PASSWORD",
            "VERIFY_EMAIL",
            "UPDATE_PROFILE",
            "CONFIGURE_TOTP"
    );

    private static final Pattern EMAIL =
            Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");


    private final KeycloakAdminClient keycloak;

    private final String adminRole;
    private final String frontendClientId;
    private final String frontendUrl;


    public BenutzerverwaltungService(
            KeycloakAdminClient keycloak,

            @Value("${foodcoops.keycloak.admin-role:Admin}")
            String adminRole,

            @Value("${foodcoops.keycloak.frontend-client-id:foodcoop-pwa}")
            String frontendClientId,

            @Value("${foodcoops.keycloak.frontend-url:}")
            String frontendUrl
    ) {
        this.keycloak = keycloak;
        this.adminRole = adminRole;
        this.frontendClientId = frontendClientId;
        this.frontendUrl = frontendUrl;
    }


    // =========================================================================
    // Benutzer
    // =========================================================================

    public List<Benutzer> listUsers() {
        return listUsers(null);
    }


    /**
     * @param angemeldet ID des angemeldeten Admins – hilft, die Admin-Rolle
     *                   auch ohne "view-realm" zu finden
     */
    public List<Benutzer> listUsers(String angemeldet) {
        List<Role> roles = manageableRoles();
        List<User> alle = keycloak.listUsers();

        Map<String, List<String>> rolesByUser = new HashMap<>();

        for (Role role : roles) {
            for (User member : keycloak.getRoleUsers(role.name())) {
                rolesByUser
                        .computeIfAbsent(member.id(), id -> new ArrayList<>())
                        .add(role.name());
            }
        }

        if (realmAdminRole(roles, angemeldet) != null) {
            for (String id : realmAdminIds(alle)) {
                rolesByUser
                        .computeIfAbsent(id, key -> new ArrayList<>())
                        .add(adminRole);
            }
        }

        return alle
                .stream()
                .filter(user -> !isServiceAccount(user))
                .map(user -> toBenutzer(
                        user,
                        rolesByUser.getOrDefault(user.id(), List.of())
                ))
                .sorted(Comparator.comparing(
                        Benutzer::username,
                        String.CASE_INSENSITIVE_ORDER
                ))
                .toList();
    }


    public Benutzer getUser(String userId) {
        User user = keycloak.getUser(userId);

        return toBenutzer(user, userRoleNames(userId));
    }


    public Benutzer createUser(BenutzerAnlegen request) {
        return createUser(request, null);
    }


    public Benutzer createUser(BenutzerAnlegen request, String angemeldet) {
        String username = trimToNull(request.username());

        if (username == null) {
            throw badRequest("Bitte einen Benutzernamen angeben.");
        }

        String email = trimToNull(request.email());
        validateEmail(email);

        // Neue Benutzer brauchen immer eine Adresse – auch mit Startpasswort
        if (email == null) {
            throw badRequest("Bitte eine E-Mail-Adresse angeben.");
        }

        boolean setupEmail = Boolean.TRUE.equals(request.sendSetupEmail());

        List<Role> manageable = manageableRoles();
        Role realmAdmin = realmAdminRole(manageable, angemeldet);
        List<Role> rolesToAssign = resolveRoles(request.roles(), manageable, realmAdmin);

        String password = request.password();

        List<Credential> credentials =
                StringUtils.hasText(password)
                        ? List.of(Credential.password(
                                password,
                                !Boolean.FALSE.equals(request.temporaryPassword())
                        ))
                        : null;

        User user = new User(
                null,
                username,
                email,
                trimToNull(request.firstName()),
                trimToNull(request.lastName()),
                !Boolean.FALSE.equals(request.enabled()),
                null,
                null,
                null,
                credentials
        );

        String userId = keycloak.createUser(user);

        keycloak.addUserRoles(userId, clientRoles(rolesToAssign, realmAdmin));

        if (rolesToAssign.contains(realmAdmin)) {
            keycloak.addUserRealmRoles(userId, List.of(realmAdmin));
        }

        if (setupEmail) {
            List<String> actions =
                    StringUtils.hasText(password)
                            ? List.of("VERIFY_EMAIL")
                            : List.of("UPDATE_PASSWORD", "VERIFY_EMAIL");

            sendActionsEmail(userId, actions);
        }

        return getUser(userId);
    }


    public Benutzer updateUser(
            String userId,
            BenutzerAendern request,
            String currentUserId
    ) {
        if (Boolean.FALSE.equals(request.enabled())
                && userId.equals(currentUserId)) {
            throw badRequest("Du kannst dein eigenes Konto nicht deaktivieren.");
        }

        User existing = keycloak.getUser(userId);

        /* null = unverändert, "" = E-Mail entfernen */
        String email =
                request.email() == null
                        ? existing.email()
                        : trimToNull(request.email());

        validateEmail(email);

        boolean emailChanged =
                !Objects.equals(
                        normalize(existing.email()),
                        normalize(email)
                );

        /*
         * Vollständige Repräsentation senden: Seit dem User Profile
         * (Keycloak 24+) können fehlende Felder sonst geleert werden.
         */
        User update = new User(
                existing.id(),
                existing.username(),
                email,
                request.firstName() == null
                        ? existing.firstName()
                        : trimToNull(request.firstName()),
                request.lastName() == null
                        ? existing.lastName()
                        : trimToNull(request.lastName()),
                request.enabled() == null
                        ? existing.enabled()
                        : request.enabled(),
                emailChanged
                        ? Boolean.FALSE
                        : existing.emailVerified(),
                null,
                existing.requiredActions(),
                null
        );

        keycloak.updateUser(userId, update);

        return getUser(userId);
    }


    public void deleteUser(String userId, String currentUserId) {
        if (userId.equals(currentUserId)) {
            throw badRequest("Du kannst dein eigenes Konto nicht löschen.");
        }

        keycloak.deleteUser(userId);
    }


    public Benutzer setUserRoles(
            String userId,
            List<String> roleNames,
            String currentUserId
    ) {
        List<Role> manageable = manageableRoles();
        Role realmAdmin = realmAdminRole(manageable, currentUserId);
        List<Role> target = resolveRoles(roleNames, manageable, realmAdmin);

        Set<String> targetNames = new LinkedHashSet<>();
        target.forEach(role -> targetNames.add(role.name()));

        List<Role> current =
                keycloak
                        .getUserRoles(userId)
                        .stream()
                        .filter(this::isManageable)
                        .toList();

        Set<String> currentNames = new LinkedHashSet<>();
        current.forEach(role -> currentNames.add(role.name()));

        boolean hatRealmAdmin =
                realmAdmin != null && hasRealmRole(userId, adminRole);

        if (hatRealmAdmin) {
            currentNames.add(adminRole);
        }

        /*
         * Die eigene Admin-Rolle bleibt immer erhalten – sonst würde man sich
         * aussperren. Das Frontend zeigt sie beim eigenen Konto gesperrt an.
         */
        if (userId.equals(currentUserId)
                && currentNames.contains(adminRole)) {
            targetNames.add(adminRole);
        }

        keycloak.removeUserRoles(
                userId,
                current.stream()
                        .filter(role -> !targetNames.contains(role.name()))
                        .toList()
        );

        keycloak.addUserRoles(
                userId,
                clientRoles(target, realmAdmin)
                        .stream()
                        .filter(role -> !currentNames.contains(role.name()))
                        .toList()
        );

        if (realmAdmin != null) {
            boolean sollAdmin = targetNames.contains(adminRole);

            if (sollAdmin && !hatRealmAdmin) {
                keycloak.addUserRealmRoles(userId, List.of(realmAdmin));
            } else if (!sollAdmin && hatRealmAdmin) {
                keycloak.removeUserRealmRoles(userId, List.of(realmAdmin));
            }
        }

        return getUser(userId);
    }


    public void setPassword(
            String userId,
            String password,
            Boolean temporary
    ) {
        if (!StringUtils.hasText(password)) {
            throw badRequest("Bitte ein Passwort angeben.");
        }

        keycloak.resetPassword(
                userId,
                Credential.password(
                        password,
                        !Boolean.FALSE.equals(temporary)
                )
        );
    }


    public void sendActionsEmail(String userId, List<String> actions) {
        if (actions == null || actions.isEmpty()) {
            throw badRequest("Bitte mindestens eine Aktion auswählen.");
        }

        for (String action : actions) {
            if (!ERLAUBTE_AKTIONEN.contains(action)) {
                throw badRequest("Unbekannte Aktion: " + action);
            }
        }

        User user = keycloak.getUser(userId);

        if (!StringUtils.hasText(user.email())) {
            throw badRequest(
                    "Der Benutzer hat keine E-Mail-Adresse hinterlegt."
            );
        }

        keycloak.executeActionsEmail(
                userId,
                actions,
                frontendClientId,
                trimToNull(frontendUrl)
        );
    }


    public void logout(String userId) {
        keycloak.logout(userId);
    }


    /**
     * Benutzer mit einer bestimmten Rolle, z.B. als E-Mail-Empfänger.
     */
    public List<BenutzerKontakt> usersOfRole(String roleName) {
        List<User> mitglieder;

        try {
            mitglieder = keycloak.getRoleUsers(roleName);
        } catch (KeycloakAdminException exception) {
            if (exception.getStatus() != HttpStatus.NOT_FOUND) {
                throw exception;
            }

            // Rolle gibt es (noch) nicht, z.B. "Einkaufsmanagement" – dann niemand
            LOG.info("Rolle \"{}\" gibt es in Keycloak nicht – keine Empfänger.", roleName);
            return List.of();
        }

        return mitglieder
                .stream()
                .filter(user -> !Boolean.FALSE.equals(user.enabled()))
                .map(user -> new BenutzerKontakt(
                        user.id(),
                        user.username(),
                        user.email(),
                        user.firstName(),
                        user.lastName()
                ))
                .toList();
    }


    // =========================================================================
    // Rollen
    // =========================================================================

    public List<Rolle> listRoles() {
        return listRoles(null);
    }


    public List<Rolle> listRoles(String angemeldet) {
        List<Role> manageable = manageableRoles();

        List<Rolle> rollen =
                new ArrayList<>(
                        manageable
                                .stream()
                                .map(role -> new Rolle(
                                        role.name(),
                                        role.description(),
                                        keycloak.getRoleUsers(role.name()).size(),
                                        adminRole.equals(role.name())
                                ))
                                .toList()
                );

        Role realmAdmin = realmAdminRole(manageable, angemeldet);

        if (realmAdmin != null) {
            rollen.add(new Rolle(
                    adminRole,
                    realmAdmin.description(),
                    anzahlRealmAdmins(),
                    true
            ));
        } else if (manageable.stream().noneMatch(role -> adminRole.equals(role.name()))) {
            LOG.warn(
                    "Die Admin-Rolle \"{}\" ist weder Client-Rolle noch als Realm-Rolle lesbar "
                            + "und kann deshalb nicht vergeben werden.",
                    adminRole
            );
        }

        rollen.sort(Comparator.comparing(
                Rolle::name,
                String.CASE_INSENSITIVE_ORDER
        ));

        return rollen;
    }


    // =========================================================================
    // Intern
    // =========================================================================

    private List<Role> manageableRoles() {
        return keycloak
                .listRoles()
                .stream()
                .filter(this::isManageable)
                .toList();
    }


    private List<String> userRoleNames(String userId) {
        List<String> namen =
                new ArrayList<>(
                        keycloak
                                .getUserRoles(userId)
                                .stream()
                                .filter(this::isManageable)
                                .map(Role::name)
                                .toList()
                );

        // Die Person selbst dient als Hinweis: hat sie Admin, wird die Rolle so gefunden
        if (!namen.contains(adminRole)
                && realmAdminRole(manageableRoles(), userId) != null
                && hasRealmRole(userId, adminRole)) {
            namen.add(adminRole);
        }

        namen.sort(String.CASE_INSENSITIVE_ORDER);
        return namen;
    }


    /**
     * Die Admin-Rolle als Realm-Rolle – nur wenn sie nicht schon unter den
     * verwalteten (Client-)Rollen ist. Sonst null.
     *
     * Ohne "view-realm" darf das Backend Realm-Rollen nicht direkt lesen.
     * Dann wird die Rolle aus den Realm-Rollen einer Person genommen, die
     * sie hat (z.B. der angemeldete Admin) – dafür reicht "view-users".
     */
    private Role realmAdminRole(List<Role> manageable, String personMitAdmin) {
        if (!keycloak.usesClientRoles()
                || manageable.stream().anyMatch(role -> adminRole.equals(role.name()))) {
            return null;
        }

        try {
            Role role = keycloak.findRealmRole(adminRole);

            if (role != null) {
                return role;
            }
        } catch (KeycloakAdminException exception) {
            LOG.info("Admin-Rolle nicht direkt lesbar ({}) – suche sie über die Person.", exception.getMessage());
        }

        if (personMitAdmin == null) {
            return null;
        }

        try {
            return keycloak.getUserRealmRoles(personMitAdmin)
                    .stream()
                    .filter(role -> adminRole.equals(role.name()))
                    .findFirst()
                    .orElse(null);
        } catch (KeycloakAdminException exception) {
            LOG.warn("Admin-Rolle als Realm-Rolle nicht lesbar: {}", exception.getMessage());
            return null;
        }
    }


    /** IDs aller Personen mit der Realm-Admin-Rolle. */
    private Set<String> realmAdminIds(List<User> kandidaten) {
        try {
            Set<String> ids = new HashSet<>();
            keycloak.getRealmRoleUsers(adminRole).forEach(user -> ids.add(user.id()));
            return ids;
        } catch (KeycloakAdminException exception) {
            // Ohne "view-realm": je Person nachsehen (langsamer, aber korrekt)
            Set<String> ids = new HashSet<>();

            for (User user : kandidaten) {
                if (!isServiceAccount(user) && hasRealmRole(user.id(), adminRole)) {
                    ids.add(user.id());
                }
            }

            return ids;
        }
    }


    private int anzahlRealmAdmins() {
        try {
            return keycloak.getRealmRoleUsers(adminRole).size();
        } catch (KeycloakAdminException exception) {
            return 0;
        }
    }


    private boolean hasRealmRole(String userId, String roleName) {
        return keycloak
                .getUserRealmRoles(userId)
                .stream()
                .anyMatch(role -> roleName.equals(role.name()));
    }


    /** Rollen ohne die Realm-Admin-Rolle (die läuft über die Realm-API). */
    private static List<Role> clientRoles(List<Role> roles, Role realmAdmin) {
        return roles.stream()
                .filter(role -> role != realmAdmin)
                .toList();
    }


    private List<Role> resolveRoles(
            List<String> names,
            List<Role> manageable,
            Role realmAdmin
    ) {
        if (names == null || names.isEmpty()) {
            return List.of();
        }

        Map<String, Role> available = new HashMap<>();
        manageable.forEach(role -> available.put(role.name(), role));

        if (realmAdmin != null) {
            available.put(adminRole, realmAdmin);
        }

        List<Role> result = new ArrayList<>();

        for (String name : new LinkedHashSet<>(names)) {
            Role role = available.get(name);

            if (role == null) {
                throw badRequest("Unbekannte Rolle: " + name);
            }

            result.add(role);
        }

        return result;
    }


    private boolean isManageable(Role role) {
        return isManageableName(role.name());
    }


    private boolean isManageableName(String name) {
        return name != null
                && !name.startsWith("default-roles-")
                && !name.equals("offline_access")
                && !name.equals("uma_authorization");
    }


    private static boolean isServiceAccount(User user) {
        return user.username() != null
                && user.username().startsWith("service-account-");
    }


    private static Benutzer toBenutzer(User user, List<String> roles) {
        return new Benutzer(
                user.id(),
                user.username(),
                user.email(),
                user.firstName(),
                user.lastName(),
                !Boolean.FALSE.equals(user.enabled()),
                Boolean.TRUE.equals(user.emailVerified()),
                user.createdTimestamp(),
                user.requiredActions() == null
                        ? List.of()
                        : user.requiredActions(),
                roles.stream()
                        .sorted(String.CASE_INSENSITIVE_ORDER)
                        .toList()
        );
    }


    private static void validateEmail(String email) {
        if (email != null && !EMAIL.matcher(email).matches()) {
            throw badRequest("Bitte eine gültige E-Mail-Adresse angeben.");
        }
    }


    private static String normalize(String value) {
        String trimmed = trimToNull(value);
        return trimmed == null ? null : trimmed.toLowerCase();
    }


    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }


    private static KeycloakAdminException badRequest(String message) {
        return new KeycloakAdminException(HttpStatus.BAD_REQUEST, message);
    }
}
