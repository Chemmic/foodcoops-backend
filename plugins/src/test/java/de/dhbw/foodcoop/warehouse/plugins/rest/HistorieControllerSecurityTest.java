package de.dhbw.foodcoop.warehouse.plugins.rest;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.dhbw.foodcoop.warehouse.CorsConfiguration;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieDtos.Historie;
import de.dhbw.foodcoop.warehouse.plugins.historie.HistorieService;
import de.dhbw.foodcoop.warehouse.plugins.security.SecurityConfiguration;

@WebMvcTest(controllers = HistorieController.class)
@Import({
        SecurityConfiguration.class,
        CorsConfiguration.class
})
class HistorieControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private HistorieService service;

    @MockitoBean
    private DeadlineService deadlineService;


    @Test
    void ownHistoryRequiresLogin() throws Exception {
        mvc.perform(get("/me/historie"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownHistoryUsesUsernameFromToken() throws Exception {
        when(service.historie("anna"))
                .thenReturn(new Historie("anna", List.of(), List.of()));

        mvc.perform(get("/me/historie")
                        .with(jwt().jwt(token -> token.claim("preferred_username", "anna"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personId").value("anna"));

        verify(service).historie("anna");
    }

    @Test
    void adminEndpointsRejectMembers() throws Exception {
        mvc.perform(get("/admin/mitglieder")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Einkäufer"))))
                .andExpect(status().isForbidden());

        mvc.perform(get("/admin/statistik"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminCanReadOtherHistories() throws Exception {
        when(service.historie("bert"))
                .thenReturn(new Historie("bert", List.of(), List.of()));

        mvc.perform(get("/admin/mitglieder/bert/historie")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.personId").value("bert"));
    }
}
