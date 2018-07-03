package server;

import akka.actor.AbstractActor;
import ServerOnlyMessages.EmailRequest;
import org.simplejavamail.email.Email;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.simplejavamail.mailer.config.TransportStrategy;

public class EmailActor extends AbstractActor {
    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(EmailRequest.class, m -> {
                    Email email = EmailBuilder.startingBlank()
                            .from("MiniProject Chat", "miniprojecttcs@gmail.com")
                            .to("new client", m.getEmail())
                            .withSubject("Email verification - MiniProject")
                            .withPlainText("Please enter the following string on your client: /validate "
                                    + m.getEmail() + " "
                                    + m.getRandomizedString())
                            .buildEmail();

                    MailerBuilder
                            .withSMTPServer("smtp.gmail.com", 587, "miniprojecttcs", "MichaelAmitay25")
                            .withTransportStrategy(TransportStrategy.SMTP_TLS)
                            .buildMailer().sendMail(email);
                })
                .build();
    }
}
