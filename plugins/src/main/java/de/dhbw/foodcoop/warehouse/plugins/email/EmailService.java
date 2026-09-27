package de.dhbw.foodcoop.warehouse.plugins.email;

import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Versand von E-Mails über den SMTP-Zugang aus spring.mail.*.
 *
 * Absender: foodcoops.mail.from (MAIL_FROM), sonst der SMTP-Benutzer.
 * Die meisten Anbieter verlangen, dass der Absender zum SMTP-Konto passt –
 * sonst werden Mails abgelehnt oder landen im Spam.
 */
@Service
public class EmailService {

    private final JavaMailSender mailSender;
    private final String absender;
    private final String absenderName;


    /**
     * Leere Werte (z.B. MAIL_FROM= in der .env) fallen auf SMTP-Benutzer
     * bzw. Namen der Foodcoop zurück.
     */
    public EmailService(
            JavaMailSender mailSender,

            @Value("${foodcoops.mail.from:}")
            String absender,

            @Value("${foodcoops.mail.from-name:}")
            String absenderName,

            @Value("${spring.mail.username:}")
            String smtpBenutzer,

            @Value("${foodcoops.name:}")
            String foodcoopName
    ) {
        this.mailSender = mailSender;
        this.absender = ersterWert(absender, smtpBenutzer);
        this.absenderName = ersterWert(absenderName, foodcoopName);
    }


    private static String ersterWert(String... werte) {
        for (String wert : werte) {
            if (StringUtils.hasText(wert)) {
                return wert.trim();
            }
        }

        return "";
    }


    public void sendSimpleMessage(String to, String subject, String text) {
        SimpleMailMessage message = new SimpleMailMessage();

        InternetAddress von = absender();
        if (von != null) {
            message.setFrom(von.toString());
        }

        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }


    /** Mail mit Text- und HTML-Fassung; das Mailprogramm zeigt, was es kann. */
    public void sendTextUndHtml(String to, String subject, String text, String html) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        InternetAddress von = absender();
        if (von != null) {
            helper.setFrom(von);
        }

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text, html);
        mailSender.send(message);
    }


    public void sendEmailWithPDF(String to, String subject, String text, byte[] pdfBytes, String pdfFileName) throws MessagingException {
        MimeMessage message = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());

        InternetAddress von = absender();
        if (von != null) {
            helper.setFrom(von);
        }

        helper.setTo(to);
        helper.setSubject(subject);
        helper.setText(text);

        ByteArrayResource pdfResource = new ByteArrayResource(pdfBytes);
        helper.addAttachment(pdfFileName, pdfResource);
        mailSender.send(message);
    }


    /** Absender mit Anzeigename; null = Vorgabe des Mailservers. */
    InternetAddress absender() {
        if (!StringUtils.hasText(absender)) {
            return null;
        }

        try {
            return StringUtils.hasText(absenderName)
                    ? new InternetAddress(absender, absenderName, StandardCharsets.UTF_8.name())
                    : new InternetAddress(absender);
        } catch (UnsupportedEncodingException | jakarta.mail.internet.AddressException exception) {
            throw new IllegalStateException(
                    "Ungültige Absenderadresse in MAIL_FROM: " + absender, exception);
        }
    }
}
