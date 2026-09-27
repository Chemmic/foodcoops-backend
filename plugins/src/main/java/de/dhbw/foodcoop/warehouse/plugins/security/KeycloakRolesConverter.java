package de.dhbw.foodcoop.warehouse.plugins.security;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Übersetzt die Keycloak-Rollen eines Access Tokens in Spring-Authorities.
 *
 * Berücksichtigt werden – wie im Frontend (hasAnyRole) –
 * Realm-Rollen und Client-Rollen:
 *
 *   realm_access.roles           -> ROLE_<name>
 *   resource_access.*.roles      -> ROLE_<name>
 */
public class KeycloakRolesConverter
        implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();

        addRoles(
                jwt.getClaimAsMap("realm_access"),
                authorities
        );

        Map<String, Object> resourceAccess =
                jwt.getClaimAsMap("resource_access");

        if (resourceAccess != null) {
            for (Object client : resourceAccess.values()) {
                if (client instanceof Map<?, ?> clientAccess) {
                    addRoles(clientAccess, authorities);
                }
            }
        }

        return authorities;
    }


    private static void addRoles(
            Map<?, ?> access,
            Set<GrantedAuthority> authorities
    ) {
        if (access == null) {
            return;
        }

        if (access.get("roles") instanceof Collection<?> roles) {
            for (Object role : roles) {
                if (role != null) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + role)
                    );
                }
            }
        }
    }
}
