package com.ko.footballupdater.services;

import com.ko.footballupdater.models.InstagramPost;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EmailService {

    Logger LOG = LoggerFactory.getLogger(EmailService.class);

    @Value("${mailer.subject}")
    private String mailSubject;

    @Value("${mailer.from.name}")
    private String fromName;

    @Value("${mailer.from.address}")
    private String fromAddress;

    @Value("${mailer.from.password}")
    private String fromAddressPassword;

    @Value("${mailer.to.name}")
    private String toName;

    @Value("${mailer.to.address}")
    private String toAddress;

//    @EventListener(ApplicationReadyEvent.class)
//    public void validateMailerOnStartUp() {
//        mailer.testConnection();
//    }

    public boolean sendEmailUpdate(List<InstagramPost> posts){
        // Create email body
        StringBuilder emailContent = new StringBuilder();
        for (InstagramPost post : posts) {
            emailContent.append("############").append(post.getPlayer().getName()).append("############\n\n");
            emailContent.append(post.getCaption()).append("\n");
            emailContent.append(post.getImageSearchUrl()).append("\n\n\n");
        }
        try {
            Email email = EmailBuilder.startingBlank()
                    .from(fromName, fromAddress)
                    .to(toName, toAddress)
                    .withSubject(mailSubject)
                    .withPlainText(emailContent.toString())
                    .buildEmail();

            Mailer mailer = MailerBuilder
                    .withSMTPServer("smtp.gmail.com", 587, fromAddress, fromAddressPassword)
                    .withTransportStrategy(TransportStrategy.SMTP_TLS)
                    .withSessionTimeout(10 * 1000)
                    .clearEmailValidator()
                    .withDebugLogging(true)
                    .buildMailer();

//            mailer.testConnection();
            mailer.validate(email);
            mailer.sendMail(email);
        } catch (Exception ex) {
            LOG.warn("Sending email failed with " + ex);
            return false;
        }
        return true;
    }
}
