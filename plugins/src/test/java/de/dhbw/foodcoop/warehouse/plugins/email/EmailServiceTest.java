package de.dhbw.foodcoop.warehouse.plugins.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Properties;

import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

class EmailServiceTest {

    private final JavaMailSender sender = mock(JavaMailSender.class);


    @Test
    void usesConfiguredSenderWithDisplayName() throws Exception {
        EmailService service = new EmailService(sender, "foodcoop@example.org", "FoodCoop MiKa", "", "");

        service.sendSimpleMessage("anna@example.org", "Betreff", "Text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());

        InternetAddress von = new InternetAddress(captor.getValue().getFrom());
        assertThat(von.getAddress()).isEqualTo("foodcoop@example.org");
        assertThat(von.getPersonal()).isEqualTo("FoodCoop MiKa");
    }


    @Test
    void pdfMailUsesTheSameSender() throws Exception {
        when(sender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        EmailService service = new EmailService(sender, "foodcoop@example.org", "", "", "");

        service.sendEmailWithPDF("anna@example.org", "Rechnung", "Anbei", new byte[] { 1, 2 }, "rechnung.pdf");

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(sender).send(captor.capture());

        assertThat(((InternetAddress) captor.getValue().getFrom()[0]).getAddress())
                .isEqualTo("foodcoop@example.org");
    }


    @Test
    void withoutSenderTheMailServerDecides() {
        EmailService service = new EmailService(sender, "", "", "", "");

        service.sendSimpleMessage("anna@example.org", "Betreff", "Text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());
        assertThat(captor.getValue().getFrom()).isNull();
    }


    @Test
    void emptyValuesFallBackToSmtpUserAndFoodcoopName() throws Exception {
        // So kommt es aus compose.yml, wenn MAIL_FROM / MAIL_FROM_NAME leer sind
        EmailService service = new EmailService(sender, "", " ", "webmaster@foodcoop.fun", "FoodCoop MiKa");

        service.sendSimpleMessage("anna@example.org", "Betreff", "Text");

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(sender).send(captor.capture());

        InternetAddress von = new InternetAddress(captor.getValue().getFrom());
        assertThat(von.getAddress()).isEqualTo("webmaster@foodcoop.fun");
        assertThat(von.getPersonal()).isEqualTo("FoodCoop MiKa");
    }


    @Test
    void invalidSenderGivesClearMessage() {
        EmailService service = new EmailService(sender, "kein adresse @", "", "", "");

        assertThatThrownBy(() -> service.sendSimpleMessage("a@b.de", "x", "y"))
                .hasMessageContaining("MAIL_FROM");
    }
}
