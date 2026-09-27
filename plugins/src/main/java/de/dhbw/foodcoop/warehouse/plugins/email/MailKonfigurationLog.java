package de.dhbw.foodcoop.warehouse.plugins.email;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Schreibt beim Start einmal ins Log, über welchen Server und mit welchem
 * Absender Mails verschickt werden – ohne Passwort. Hilft, falsche oder
 * fehlende Einstellungen in der .env schnell zu erkennen.
 */
@Component
public class MailKonfigurationLog {

    private static final Logger LOG =
            LoggerFactory.getLogger(MailKonfigurationLog.class);

    private final String host;
    private final String port;
    private final String benutzer;
    private final boolean passwortGesetzt;
    private final String absender;


    public MailKonfigurationLog(
            @Value("${spring.mail.host:}") String host,
            @Value("${spring.mail.port:}") String port,
            @Value("${spring.mail.username:}") String benutzer,
            @Value("${spring.mail.password:}") String passwort,
            @Value("${foodcoops.mail.from:}") String absender
    ) {
        this.host = host;
        this.port = port;
        this.benutzer = benutzer;
        this.passwortGesetzt = StringUtils.hasText(passwort);
        this.absender = StringUtils.hasText(absender) ? absender : benutzer;
    }


    @EventListener(ApplicationReadyEvent.class)
    public void melden() {
        if (!StringUtils.hasText(benutzer) || !passwortGesetzt) {
            LOG.warn(
                    "Mailversand über {}:{} – SMTP-Benutzer oder -Passwort fehlt "
                            + "(SPRING_MAIL_USERNAME / SPRING_MAIL_PASSWORD). Mails werden vermutlich abgelehnt.",
                    host, port
            );
            return;
        }

        LOG.info("Mailversand über {}:{} als {} (Absender {}).", host, port, benutzer, absender);
    }
}
