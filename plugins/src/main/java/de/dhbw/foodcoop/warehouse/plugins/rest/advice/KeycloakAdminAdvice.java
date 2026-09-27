package de.dhbw.foodcoop.warehouse.plugins.rest.advice;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import de.dhbw.foodcoop.warehouse.plugins.keycloak.BenutzerverwaltungDtos.Fehler;
import de.dhbw.foodcoop.warehouse.plugins.keycloak.KeycloakAdminException;

@ControllerAdvice
public class KeycloakAdminAdvice {

    @ResponseBody
    @ExceptionHandler(KeycloakAdminException.class)
    public ResponseEntity<Fehler> handleKeycloakAdmin(
            KeycloakAdminException exception
    ) {
        return ResponseEntity
                .status(exception.getStatus())
                .body(new Fehler(exception.getMessage()));
    }
}
