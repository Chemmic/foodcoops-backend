package de.dhbw.foodcoop.warehouse.plugins.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.BearerTokenResolver;
import org.springframework.security.oauth2.server.resource.web.DefaultBearerTokenResolver;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.util.StringUtils;

/**
 * ============================================================================
 * Spring Security / Keycloak
 * ============================================================================
 *
 * Das Backend ist ein OAuth2 Resource Server: Das Frontend schickt das
 * Keycloak Access Token als "Authorization: Bearer ..." mit, das Backend
 * prüft die Signatur über das JWK-Set von Keycloak.
 *
 * Geschützt:
 *
 *   /keycloak/admin/**   -> nur Rolle "Admin" (foodcoops.keycloak.admin-role)
 *   /keycloak/**         -> Einkäufer oder Admin
 *   /organisation/**     -> Organisator oder Admin (finale Bestellung festlegen)
 *   /admin/**            -> Admin (Verwaltung: Mitglieder, Statistik)
 *   /me/**               -> angemeldet (eigene Historie)
 *
 * Alle übrigen Endpunkte:
 *
 *   foodcoops.security.require-authentication=false (Standard)
 *       -> weiterhin offen, damit bestehende Clients nicht brechen.
 *          Tokens werden dort ignoriert.
 *
 *   foodcoops.security.require-authentication=true
 *       -> jeder Request braucht ein gültiges Token.
 *
 * ============================================================================
 */
@Configuration
@EnableWebSecurity
public class SecurityConfiguration {

    private static final Logger LOG =
            LoggerFactory.getLogger(SecurityConfiguration.class);

    /** Pfade, für die Tokens immer ausgewertet werden. */
    private static final List<String> PROTECTED_PATHS =
            List.of("/keycloak/", "/organisation/", "/me/", "/admin/");


    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,

            @Value("${foodcoops.security.require-authentication:false}")
            boolean requireAuthentication,

            @Value("${foodcoops.keycloak.admin-role:Admin}")
            String adminRole,

            @Value("${foodcoops.keycloak.einkaeufer-role:Einkäufer}")
            String einkaeuferRole,

            @Value("${foodcoops.keycloak.organisator-role:Organisator}")
            String organisatorRole
    ) throws Exception {

        http
                .csrf(csrf -> csrf.disable())

                .cors(Customizer.withDefaults())

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> {
                    auth.requestMatchers(HttpMethod.OPTIONS, "/**")
                            .permitAll();

                    auth.requestMatchers("/keycloak/admin/**")
                            .hasRole(adminRole);

                    auth.requestMatchers("/keycloak/**")
                            .hasAnyRole(einkaeuferRole, adminRole);

                    auth.requestMatchers("/organisation/**")
                            .hasAnyRole(organisatorRole, adminRole);

                    auth.requestMatchers("/admin/**")
                            .hasRole(adminRole);

                    auth.requestMatchers("/me/**")
                            .authenticated();

                    if (requireAuthentication) {
                        auth.requestMatchers(
                                        "/error",
                                        "/swagger-ui/**",
                                        "/swagger-ui.html",
                                        "/v3/api-docs/**"
                                )
                                .permitAll();

                        auth.anyRequest().authenticated();
                    } else {
                        auth.anyRequest().permitAll();
                    }
                })

                .exceptionHandling(exceptions -> exceptions
                        .accessDeniedHandler(accessDeniedHandler())
                )

                .oauth2ResourceServer(oauth2 -> oauth2
                        .bearerTokenResolver(
                                bearerTokenResolver(requireAuthentication)
                        )
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .jwt(jwt -> jwt
                                .jwtAuthenticationConverter(
                                        jwtAuthenticationConverter()
                                )
                        )
                );

        return http.build();
    }


    /**
     * 401 mit lesbarer JSON-Meldung. Der eigentliche Grund (z.B. falscher
     * Issuer, JWK-Set nicht erreichbar) landet im Log, nicht beim Client.
     */
    private static AuthenticationEntryPoint authenticationEntryPoint() {
        BearerTokenAuthenticationEntryPoint delegate =
                new BearerTokenAuthenticationEntryPoint();

        return (request, response, exception) -> {
            boolean tokenSent =
                    request.getHeader(HttpHeaders.AUTHORIZATION) != null;

            if (tokenSent) {
                LOG.warn(
                        "Keycloak-Token für {} abgelehnt: {}",
                        request.getRequestURI(),
                        rootCauseMessage(exception)
                );
            }

            delegate.commence(request, response, exception);

            writeJson(
                    response,
                    tokenSent
                            ? "Anmeldung nicht akzeptiert. Bitte neu anmelden."
                            : "Bitte melde dich an."
            );
        };
    }


    private static AccessDeniedHandler accessDeniedHandler() {
        return (request, response, exception) -> {
            response.setStatus(HttpStatus.FORBIDDEN.value());

            writeJson(
                    response,
                    "Dafür fehlt dir die nötige Rolle."
            );
        };
    }


    private static void writeJson(
            HttpServletResponse response,
            String message
    ) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // Nur feste Texte – kein Escaping von Benutzereingaben nötig.
        response.getWriter().write(
                "{\"message\":\"" + message.replace("\"", "\\\"") + "\"}"
        );
    }


    private static String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;

        while (current.getCause() != null
                && current.getCause() != current) {
            current = current.getCause();
        }

        return current.getClass().getSimpleName()
                + ": " + current.getMessage();
    }


    /**
     * Solange nicht alle Endpunkte geschützt sind, werden Tokens nur für
     * /keycloak/** ausgewertet. So führt z.B. ein abgelaufenes Token auf
     * einem offenen Endpunkt nicht zu einem 401.
     */
    private static BearerTokenResolver bearerTokenResolver(
            boolean requireAuthentication
    ) {
        DefaultBearerTokenResolver delegate =
                new DefaultBearerTokenResolver();

        if (requireAuthentication) {
            return delegate;
        }

        return request -> {
            String path =
                    request.getRequestURI()
                            .substring(request.getContextPath().length());

            return PROTECTED_PATHS.stream().anyMatch(path::startsWith)
                    ? delegate.resolve(request)
                    : null;
        };
    }


    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter =
                new JwtAuthenticationConverter();

        converter.setJwtGrantedAuthoritiesConverter(
                new KeycloakRolesConverter()
        );

        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }


    /**
     * Eigener JwtDecoder, damit leere Umgebungsvariablen den Start nicht
     * verhindern.
     *
     * Reihenfolge:
     *
     *   1. jwk-set-uri (+ optional issuer-uri)  – Docker: JWK-Set intern
     *      laden (http://keycloak:8080/...), Issuer = öffentliche URL
     *   2. issuer-uri
     *   3. abgeleitet aus foodcoops.keycloak.server-url + realm
     *      – lokal genügt damit KEYCLOAK_SERVER_URL
     */
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
            String jwkSetUri,

            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:}")
            String issuerUri,

            @Value("${foodcoops.keycloak.server-url:}")
            String keycloakServerUrl,

            @Value("${foodcoops.keycloak.realm:foodcoop}")
            String realm
    ) {
        if (StringUtils.hasText(jwkSetUri)) {
            LOG.info(
                    "Keycloak-Tokens: JWK-Set {}, Issuer {}",
                    jwkSetUri,
                    StringUtils.hasText(issuerUri) ? issuerUri : "(nicht geprüft)"
            );

            NimbusJwtDecoder decoder =
                    NimbusJwtDecoder
                            .withJwkSetUri(jwkSetUri)
                            .build();

            if (StringUtils.hasText(issuerUri)) {
                decoder.setJwtValidator(
                        JwtValidators.createDefaultWithIssuer(issuerUri)
                );
            }

            return withReadableErrors(decoder, jwkSetUri);
        }

        String issuer =
                StringUtils.hasText(issuerUri)
                        ? issuerUri
                        : StringUtils.hasText(keycloakServerUrl)
                                ? keycloakServerUrl.replaceAll("/+$", "")
                                        + "/realms/" + realm
                                : null;

        if (issuer != null) {
            LOG.info("Keycloak-Tokens: Issuer {}", issuer);

            // Lazy: Keycloak muss beim Start des Backends noch nicht laufen.
            return withReadableErrors(
                    new SupplierJwtDecoder(
                            () -> JwtDecoders.fromIssuerLocation(issuer)
                    ),
                    issuer
            );
        }

        LOG.warn(
                "Keycloak ist nicht konfiguriert (KEYCLOAK_SERVER_URL bzw. "
                        + "issuer-uri / jwk-set-uri fehlen) - Tokens werden abgelehnt."
        );

        return token -> {
            throw new BadJwtException(
                    "JWT-Prüfung ist nicht konfiguriert"
            );
        };
    }


    /**
     * Ist Keycloak nicht erreichbar, würden Nimbus / SupplierJwtDecoder
     * Fehler werfen, die zu einem 500 führen. Als BadJwtException werden
     * sie zu einem 401 mit verständlicher Meldung (Grund im Log).
     */
    private static JwtDecoder withReadableErrors(
            JwtDecoder delegate,
            String keycloakLocation
    ) {
        return token -> {
            try {
                return delegate.decode(token);
            } catch (BadJwtException exception) {
                throw exception;
            } catch (RuntimeException exception) {
                throw new BadJwtException(
                        "Keycloak (" + keycloakLocation + ") nicht erreichbar "
                                + "oder falsch konfiguriert: "
                                + rootCauseMessage(exception),
                        exception
                );
            }
        };
    }
}
