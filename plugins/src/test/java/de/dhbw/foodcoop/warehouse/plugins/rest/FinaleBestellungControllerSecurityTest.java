package de.dhbw.foodcoop.warehouse.plugins.rest;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import de.dhbw.foodcoop.warehouse.CorsConfiguration;
import de.dhbw.foodcoop.warehouse.application.deadline.DeadlineService;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungDtos.FinaleBestellung;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungService;
import de.dhbw.foodcoop.warehouse.plugins.historie.FinaleBestellungService.FinaleBestellungException;
import de.dhbw.foodcoop.warehouse.plugins.security.SecurityConfiguration;

@WebMvcTest(controllers = FinaleBestellungController.class)
@Import({
        SecurityConfiguration.class,
        CorsConfiguration.class
})
class FinaleBestellungControllerSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private FinaleBestellungService service;

    // wird von der Application-Klasse benötigt
    @MockitoBean
    private DeadlineService deadlineService;


    @Test
    void requiresLogin() throws Exception {
        mvc.perform(get("/organisation/bestellung"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void einkaeuferMayNotDecide() throws Exception {
        mvc.perform(put("/organisation/bestellung/produkte/salat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gebinde\": 1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Einkäufer"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void organisatorAndAdminMayRead() throws Exception {
        when(service.laden(anyInt()))
                .thenReturn(new FinaleBestellung("u1", null, null, null, List.of(), List.of(), List.of()));

        mvc.perform(get("/organisation/bestellung")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Organisator"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uebersichtId").value("u1"));

        mvc.perform(get("/organisation/bestellung?verlauf=500")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Admin"))))
                .andExpect(status().isOk());

        verify(service).laden(52);
    }

    @Test
    void errorsComeBackAsReadableMessage() throws Exception {
        when(service.gebindeSetzen("salat", -1.0))
                .thenThrow(new FinaleBestellungException(HttpStatus.BAD_REQUEST, "Bitte eine Anzahl angeben."));

        mvc.perform(put("/organisation/bestellung/produkte/salat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gebinde\": -1}")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_Organisator"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bitte eine Anzahl angeben."));
    }
}
