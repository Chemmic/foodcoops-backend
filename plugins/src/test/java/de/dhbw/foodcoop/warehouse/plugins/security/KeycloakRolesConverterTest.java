package de.dhbw.foodcoop.warehouse.plugins.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

class KeycloakRolesConverterTest {

    private final KeycloakRolesConverter converter =
            new KeycloakRolesConverter();

    @Test
    void mapsRealmAndClientRoles() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", Map.of(
                        "roles", List.of("Admin", "Einkäufer")
                ))
                .claim("resource_access", Map.of(
                        "foodcoop-pwa", Map.of("roles", List.of("Einkaufsmanagement"))
                ))
                .build();

        assertThat(converter.convert(jwt))
                .extracting(GrantedAuthority::getAuthority)
                .containsExactlyInAnyOrder(
                        "ROLE_Admin",
                        "ROLE_Einkäufer",
                        "ROLE_Einkaufsmanagement"
                );
    }

    @Test
    void toleratesMissingClaims() {
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "user")
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }
}
