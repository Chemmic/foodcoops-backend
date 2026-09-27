package de.dhbw.foodcoop.warehouse.plugins.keycloak;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Benutzer;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAendern;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAnlegen;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.Role;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakModels.User;

class BenutzerverwaltungServiceTest {

    private static final Role ADMIN = role("Admin");
    private static final Role EINKAEUFER = role("Einkäufer");
    private static final Role DEFAULT = role("default-roles-foodcoop");
    private static final Role OFFLINE = role("offline_access");

    private KeycloakAdminClient keycloak;
    private BenutzerverwaltungService service;

    @BeforeEach
    void setUp() {
        keycloak = mock(KeycloakAdminClient.class);

        service = new BenutzerverwaltungService(
                keycloak, "Admin", "foodcoop-pwa", ""
        );

        when(keycloak.listRoles())
                .thenReturn(List.of(ADMIN, EINKAEUFER, DEFAULT, OFFLINE));
    }


    @Test
    void listUsersHidesTechnicalRolesAndServiceAccounts() {
        when(keycloak.listUsers()).thenReturn(List.of(
                user("1", "bert"),
                user("2", "anna"),
                user("3", "service-account-foodcoop-backend")
        ));

        when(keycloak.getRoleUsers("Admin"))
                .thenReturn(List.of(user("2", "anna")));
        when(keycloak.getRoleUsers("Einkäufer"))
                .thenReturn(List.of(user("1", "bert"), user("2", "anna")));

        List<Benutzer> users = service.listUsers();

        assertThat(users).extracting(Benutzer::username)
                .containsExactly("anna", "bert");

        assertThat(users.get(0).roles())
                .containsExactly("Admin", "Einkäufer");

        verify(keycloak, never()).getRoleUsers("offline_access");
    }


    @Test
    void setRolesOnlyTouchesManageableRoles() {
        when(keycloak.getUserRoles("u1"))
                .thenReturn(List.of(ADMIN, DEFAULT));
        when(keycloak.getUser("u1")).thenReturn(user("u1", "bert"));

        service.setUserRoles("u1", List.of("Einkäufer"), "me");

        verify(keycloak).removeUserRoles("u1", List.of(ADMIN));
        verify(keycloak).addUserRoles("u1", List.of(EINKAEUFER));
    }


    @Test
    void selfCanChangeRolesIfAdminRoleIsManagedElsewhere() {
        // z.B. Admin als Realm-Rolle, verwaltet werden Client-Rollen
        when(keycloak.getUserRoles("me")).thenReturn(List.of());
        when(keycloak.getUser("me")).thenReturn(user("me", "ich"));

        service.setUserRoles("me", List.of("Einkäufer"), "me");

        verify(keycloak).addUserRoles("me", List.of(EINKAEUFER));
    }


    @Test
    void ownAdminRoleIsKeptWhenEditingOwnRoles() {
        when(keycloak.getUserRoles("me")).thenReturn(List.of(ADMIN));
        when(keycloak.getUser("me")).thenReturn(user("me", "anna"));

        // Dialog schickt Admin nicht mit – Admin bleibt trotzdem
        service.setUserRoles("me", List.of("Einkäufer"), "me");

        verify(keycloak).removeUserRoles("me", List.of());
        verify(keycloak).addUserRoles("me", List.of(EINKAEUFER));
    }


    @Test
    void adminCannotDeleteOrDisableThemselves() {
        assertThatThrownBy(() -> service.deleteUser("me", "me"))
                .isInstanceOf(KeycloakAdminException.class);

        assertThatThrownBy(() -> service.updateUser(
                "me",
                new BenutzerAendern(null, null, null, false),
                "me"
        )).isInstanceOf(KeycloakAdminException.class);

        verify(keycloak, never()).deleteUser(anyString());
    }


    @Test
    void unknownRolesAreRejected() {
        assertThatThrownBy(() -> service.setUserRoles(
                "u1", List.of("offline_access"), "me"
        ))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("Unbekannte Rolle");
    }


    @Test
    void createUserWithTemporaryPasswordAndRoles() {
        when(keycloak.createUser(any())).thenReturn("new-id");
        when(keycloak.getUser("new-id")).thenReturn(user("new-id", "clara"));
        when(keycloak.getUserRoles("new-id")).thenReturn(List.of(EINKAEUFER));

        Benutzer created = service.createUser(new BenutzerAnlegen(
                " clara ", "clara@example.org", "Clara", null,
                null, List.of("Einkäufer"),
                "Start1234", null, false
        ));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(keycloak).createUser(captor.capture());

        User sent = captor.getValue();
        assertThat(sent.username()).isEqualTo("clara");
        assertThat(sent.enabled()).isTrue();
        assertThat(sent.credentials()).singleElement()
                .satisfies(credential -> {
                    assertThat(credential.value()).isEqualTo("Start1234");
                    assertThat(credential.temporary()).isTrue();
                });

        verify(keycloak).addUserRoles("new-id", List.of(EINKAEUFER));
        verify(keycloak, never()).executeActionsEmail(any(), any(), any(), any());

        assertThat(created.roles()).containsExactly("Einkäufer");
    }

    @Test
    void createUserVerlangtImmerEineEmailAdresse() {
        assertThatThrownBy(() -> service.createUser(new BenutzerAnlegen(
                "clara", " ", "Clara", null,
                null, List.of(),
                "Start1234", null, false
        )))
                .isInstanceOf(KeycloakAdminException.class)
                .hasMessageContaining("E-Mail-Adresse");

        verify(keycloak, never()).createUser(any());
    }


    @Test
    void updateUserSendsFullRepresentation() {
        when(keycloak.getUser("u1")).thenReturn(new User(
                "u1", "bert", "bert@example.org", "Bert", "B",
                true, true, 1L, List.of(), null
        ));

        service.updateUser(
                "u1",
                new BenutzerAendern("neu@example.org", "Berti", null, null),
                "me"
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(keycloak).updateUser(eq("u1"), captor.capture());

        User sent = captor.getValue();
        assertThat(sent.username()).isEqualTo("bert");
        assertThat(sent.firstName()).isEqualTo("Berti");
        assertThat(sent.lastName()).isEqualTo("B");
        assertThat(sent.enabled()).isTrue();
        assertThat(sent.emailVerified()).isFalse();
    }


    @Test
    void toggleEnabledKeepsOtherFields() {
        when(keycloak.getUser("u1")).thenReturn(new User(
                "u1", "bert", "bert@example.org", "Bert", "B",
                true, true, 1L, List.of("VERIFY_EMAIL"), null
        ));

        service.updateUser(
                "u1",
                new BenutzerAendern(null, null, null, false),
                "me"
        );

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(keycloak).updateUser(eq("u1"), captor.capture());

        User sent = captor.getValue();
        assertThat(sent.email()).isEqualTo("bert@example.org");
        assertThat(sent.emailVerified()).isTrue();
        assertThat(sent.firstName()).isEqualTo("Bert");
        assertThat(sent.enabled()).isFalse();
        assertThat(sent.requiredActions()).containsExactly("VERIFY_EMAIL");
    }


    @Test
    void adminCanMakeOthersAdminWithClientRole() {
        when(keycloak.getUserRoles("u2")).thenReturn(List.of(EINKAEUFER));
        when(keycloak.getUser("u2")).thenReturn(user("u2", "bert"));

        service.setUserRoles("u2", List.of("Einkäufer", "Admin"), "me");

        verify(keycloak).addUserRoles("u2", List.of(ADMIN));
        verify(keycloak, never()).addUserRealmRoles(anyString(), any());
    }


    @Test
    void adminAsRealmRoleIsOfferedAndAssignedViaRealmApi() {
        Role realmAdmin = new Role("realm-admin", "Admin", "Realm-Admin", false, false);

        // Client-Rollen ohne Admin – Admin ist eine Realm-Rolle
        when(keycloak.usesClientRoles()).thenReturn(true);
        when(keycloak.listRoles()).thenReturn(List.of(EINKAEUFER, DEFAULT));
        when(keycloak.findRealmRole("Admin")).thenReturn(realmAdmin);
        when(keycloak.getRealmRoleUsers("Admin")).thenReturn(List.of(user("me", "anna")));
        when(keycloak.getUserRoles("u2")).thenReturn(List.of(EINKAEUFER));
        when(keycloak.getUserRealmRoles("u2")).thenReturn(List.of());
        when(keycloak.getUser("u2")).thenReturn(user("u2", "bert"));

        assertThat(service.listRoles())
                .extracting(BenutzerverwaltungDtos.Rolle::name, BenutzerverwaltungDtos.Rolle::protectedRole)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Admin", true),
                        org.assertj.core.groups.Tuple.tuple("Einkäufer", false)
                );

        service.setUserRoles("u2", List.of("Einkäufer", "Admin"), "me");

        verify(keycloak).addUserRealmRoles("u2", List.of(realmAdmin));
        verify(keycloak).addUserRoles("u2", List.of());
    }


    @Test
    void ownRealmAdminRoleIsKept() {
        Role realmAdmin = new Role("realm-admin", "Admin", null, false, false);

        when(keycloak.usesClientRoles()).thenReturn(true);
        when(keycloak.listRoles()).thenReturn(List.of(EINKAEUFER));
        when(keycloak.findRealmRole("Admin")).thenReturn(realmAdmin);
        when(keycloak.getUserRoles("me")).thenReturn(List.of());
        when(keycloak.getUserRealmRoles("me")).thenReturn(List.of(realmAdmin));
        when(keycloak.getUser("me")).thenReturn(user("me", "anna"));

        service.setUserRoles("me", List.of("Einkäufer"), "me");

        verify(keycloak, never()).removeUserRealmRoles(anyString(), any());
        verify(keycloak).addUserRoles("me", List.of(EINKAEUFER));
    }


    @Test
    void realmAdminWorksWithoutViewRealmPermission() {
        Role realmAdmin = new Role("realm-admin", "Admin", null, false, false);
        KeycloakAdminException verboten =
                new KeycloakAdminException(org.springframework.http.HttpStatus.BAD_GATEWAY, "keine Berechtigung");

        // Realm-Rollen und Rollen-Mitglieder nicht lesbar, Rollen einer Person schon
        when(keycloak.usesClientRoles()).thenReturn(true);
        when(keycloak.listRoles()).thenReturn(List.of(EINKAEUFER));
        when(keycloak.findRealmRole("Admin")).thenThrow(verboten);
        when(keycloak.getRealmRoleUsers("Admin")).thenThrow(verboten);
        when(keycloak.getUserRealmRoles("me")).thenReturn(List.of(realmAdmin));
        when(keycloak.getUserRealmRoles("u2")).thenReturn(List.of());
        when(keycloak.getRoleUsers("Einkäufer")).thenReturn(List.of());
        when(keycloak.listUsers()).thenReturn(List.of(user("me", "anna"), user("u2", "bert")));
        when(keycloak.getUserRoles("u2")).thenReturn(List.of());
        when(keycloak.getUser("u2")).thenReturn(user("u2", "bert"));

        // Admin wird angeboten und bei der richtigen Person angezeigt
        assertThat(service.listRoles("me"))
                .extracting(BenutzerverwaltungDtos.Rolle::name)
                .contains("Admin");

        assertThat(service.listUsers("me"))
                .filteredOn(b -> b.id().equals("me"))
                .singleElement()
                .satisfies(b -> assertThat(b.roles()).contains("Admin"));

        // ... und kann vergeben werden
        service.setUserRoles("u2", List.of("Admin"), "me");

        verify(keycloak).addUserRealmRoles("u2", List.of(realmAdmin));
    }


    @Test
    void missingRoleMeansNoRecipients() {
        when(keycloak.getRoleUsers("Einkaufsmanagement")).thenThrow(
                new KeycloakAdminException(org.springframework.http.HttpStatus.NOT_FOUND, "Rolle fehlt"));

        assertThat(service.usersOfRole("Einkaufsmanagement")).isEmpty();
    }


    private static Role role(String name) {
        return new Role("id-" + name, name, null, false, false);
    }

    private static User user(String id, String username) {
        return new User(
                id, username, username + "@example.org", null, null,
                true, true, 1L, List.of(), null
        );
    }
}
