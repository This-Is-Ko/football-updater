package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.MailerProperties;
import com.ko.footballupdater.models.InstagramPost;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private MailerProperties mailerProperties;


//    @EventListener(ApplicationReadyEvent.class)
//    public void validateMailerOnStartUp() {
//        mailer.testConnection();
//    }

    public boolean sendEmailUpdate(List<InstagramPost> posts){
        // Create email body
        StringBuilder emailContent = new StringBuilder();
        for (InstagramPost post : posts) {
            emailContent.append("############").append(post.getPlayer().getName()).append("############\n\n");
            emailContent.append(post.getCaption()).append("\n\n");
            emailContent.append(post.getImageSearchUrl()).append("\n\n\n");
        }
        try {
            Email email = EmailBuilder.startingBlank()
                    .from(mailerProperties.getFrom().getName(), mailerProperties.getFrom().getAddress())
                    .to(mailerProperties.getTo().getName(), mailerProperties.getTo().getAddress())
                    .withSubject(mailerProperties.getSubject())
                    .withPlainText(emailContent.toString())
                    .buildEmail();

            Mailer mailer = MailerBuilder
                    .withSMTPServer("smtp.gmail.com", 587, mailerProperties.getFrom().getAddress(), mailerProperties.getFrom().getPassword())
                    .withTransportStrategy(TransportStrategy.SMTP_TLS)
                    .withSessionTimeout(10 * 1000)
                    .clearEmailValidator()
                    .withDebugLogging(true)
                    .buildMailer();

//            mailer.testConnection();
            mailer.validate(email);
            mailer.sendMail(email);
        } catch (Exception ex) {
            log.warn("Sending email failed with " + ex);
            return false;
        }
        log.info("Sending email was successful");
        return true;
    }
}
