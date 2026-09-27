package de.dhbw.foodcoop.warehouse.plugins.rest;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.AktionsEmail;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Benutzer;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAendern;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerAnlegen;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.BenutzerKontakt;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.PasswortSetzen;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.RollenSetzen;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Rolle;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungService;

/**
 * ============================================================================
 * Benutzerverwaltung (Keycloak)
 * ============================================================================
 *
 * Berechtigungen werden in der SecurityConfiguration geprüft:
 *
 *   /keycloak/admin/**   -> Admin
 *   /keycloak/roles/**   -> Einkäufer oder Admin
 *
 * ============================================================================
 */
@RestController
public class BenutzerverwaltungController {

    private final BenutzerverwaltungService service;

    public BenutzerverwaltungController(BenutzerverwaltungService service) {
        this.service = service;
    }


    // =========================================================================
    // Empfänger einer Rolle (Einkauf -> Mail an Einkaufsmanagement)
    // =========================================================================

    @GetMapping("/keycloak/roles/{roleName}/users")
    public List<BenutzerKontakt> usersOfRole(
            @PathVariable String roleName
    ) {
        return service.usersOfRole(roleName);
    }


    // =========================================================================
    // Benutzer
    // =========================================================================

    @GetMapping("/keycloak/admin/users")
    public List<Benutzer> listUsers(@AuthenticationPrincipal Jwt jwt) {
        return service.listUsers(jwt == null ? null : jwt.getSubject());
    }


    @GetMapping("/keycloak/admin/users/{id}")
    public Benutzer getUser(@PathVariable String id) {
        return service.getUser(id);
    }


    @PostMapping("/keycloak/admin/users")
    public ResponseEntity<Benutzer> createUser(
            @RequestBody BenutzerAnlegen request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(service.createUser(request, jwt == null ? null : jwt.getSubject()));
    }


    @PutMapping("/keycloak/admin/users/{id}")
    public Benutzer updateUser(
            @PathVariable String id,
            @RequestBody BenutzerAendern request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.updateUser(id, request, jwt.getSubject());
    }


    @DeleteMapping("/keycloak/admin/users/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal Jwt jwt
    ) {
        service.deleteUser(id, jwt.getSubject());

        return ResponseEntity.noContent().build();
    }


    @PutMapping("/keycloak/admin/users/{id}/roles")
    public Benutzer setRoles(
            @PathVariable String id,
            @RequestBody RollenSetzen request,
            @AuthenticationPrincipal Jwt jwt
    ) {
        return service.setUserRoles(id, request.roles(), jwt.getSubject());
    }


    @PutMapping("/keycloak/admin/users/{id}/password")
    public ResponseEntity<Void> setPassword(
            @PathVariable String id,
            @RequestBody PasswortSetzen request
    ) {
        service.setPassword(id, request.password(), request.temporary());

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/keycloak/admin/users/{id}/actions-email")
    public ResponseEntity<Void> sendActionsEmail(
            @PathVariable String id,
            @RequestBody AktionsEmail request
    ) {
        service.sendActionsEmail(id, request.actions());

        return ResponseEntity.noContent().build();
    }


    @PostMapping("/keycloak/admin/users/{id}/logout")
    public ResponseEntity<Void> logout(@PathVariable String id) {
        service.logout(id);

        return ResponseEntity.noContent().build();
    }


    // =========================================================================
    // Rollen
    // =========================================================================

    @GetMapping("/keycloak/admin/roles")
    public List<Rolle> listRoles(@AuthenticationPrincipal Jwt jwt) {
        return service.listRoles(jwt == null ? null : jwt.getSubject());
    }
}
