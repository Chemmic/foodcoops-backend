package de.dhbw.foodcoop.warehouse.plugins.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.dhbw.foodcoop.warehouse.CorsConfiguration;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerKontakt;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungService;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakAdminException;
import de.dhbw.foodcoop.warehouse.plugins.rest.advice.KeycloakAdminAdvice;
import de.dhbw.foodcoop.warehouse.plugins.security.SecurityConfiguration;

@WebMvcTest(controllers = BenutzerverwaltungController.class)
@Import({
        SecurityConfiguration.class,
        CorsConfiguration.class,
        KeycloakAdminAdvice.class
})
class BenutzerverwaltungControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private BenutzerverwaltungService service;

    /* Wird von FoodcoopWarehouseApplication benötigt. */
    @MockitoBean
    private DeadlineService deadlineService;


    @Test
    void adminEndpointsRequireToken() throws Exception {
        mvc.perform(get("/keycloak/admin/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Bitte melde dich an."));
    }

    @Test
    void invalidTokenGivesExplainingMessage() throws Exception {
        // Ohne erreichbaren Keycloak kann kein Token geprüft werden.
        mvc.perform(get("/keycloak/admin/users")
                        .header("Authorization", "Bearer kaputt"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("nicht akzeptiert")
                ));
    }

    @Test
    void adminEndpointsRejectNonAdmins() throws Exception {
        mvc.perform(get("/keycloak/admin/users")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_Einkäufer")
                        )))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(
                        "Dafür fehlt dir die nötige Rolle."
                ));
    }

    @Test
    void adminEndpointsAllowAdmins() throws Exception {
        when(service.listUsers()).thenReturn(List.of());

        mvc.perform(get("/keycloak/admin/users")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_Admin")
                        )))
                .andExpect(status().isOk());
    }

    @Test
    void roleMembersAllowedForEinkaeufer() throws Exception {
        when(service.usersOfRole("Einkaufsmanagement"))
                .thenReturn(List.of(new BenutzerKontakt(
                        "1", "anna", "anna@example.org", "Anna", "A"
                )));

        mvc.perform(get("/keycloak/roles/Einkaufsmanagement/users")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_Einkäufer")
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("anna@example.org"));
    }

    @Test
    void roleMembersRejectUsersWithoutRole() throws Exception {
        mvc.perform(get("/keycloak/roles/Einkaufsmanagement/users")
                        .with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void passesCurrentUserIdToService() throws Exception {
        mvc.perform(delete("/keycloak/admin/users/other-id")
                        .with(jwt()
                                .jwt(token -> token.subject("my-id"))
                                .authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isNoContent());

        verify(service).deleteUser(eq("other-id"), eq("my-id"));
    }

    @Test
    void serviceErrorsBecomeJsonMessages() throws Exception {
        when(service.getUser(any()))
                .thenThrow(new KeycloakAdminException(
                        HttpStatus.CONFLICT, "Schon vergeben"
                ));

        mvc.perform(get("/keycloak/admin/users/x")
                        .with(jwt().authorities(
                                new SimpleGrantedAuthority("ROLE_Admin")
                        )))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Schon vergeben"));
    }

    @Test
    void otherEndpointsStayOpenAndIgnoreInvalidTokens() throws Exception {
        mvc.perform(get("/gibt-es-nicht")
                        .header("Authorization", "Bearer kaputt"))
                .andExpect(status().isNotFound());
    }
}
