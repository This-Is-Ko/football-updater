package com.ko.footballupdater.services;

import com.ko.footballupdater.configuration.ImageGeneratorProperies;
import com.ko.footballupdater.configuration.InstagramPostProperies;
import com.ko.footballupdater.configuration.MailerProperties;
import com.ko.footballupdater.models.InstagramPost;
import com.ko.footballupdater.utils.PostHelper;
import jakarta.activation.FileDataSource;
import lombok.extern.slf4j.Slf4j;
import org.simplejavamail.api.email.AttachmentResource;
import org.simplejavamail.api.email.Email;
import org.simplejavamail.api.email.EmailPopulatingBuilder;
import org.simplejavamail.api.mailer.Mailer;
import org.simplejavamail.api.mailer.config.TransportStrategy;
import org.simplejavamail.email.EmailBuilder;
import org.simplejavamail.mailer.MailerBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class EmailService {

    @Autowired
    private MailerProperties mailerProperties;

    @Autowired
    private ImageGeneratorProperies imageGeneratorProperies;

    @Autowired
    InstagramPostProperies instagramPostProperies;

    public boolean sendEmailUpdate(List<InstagramPost> posts){
        // Check config for email enabled status
        if (!mailerProperties.isEnabled()) {
            return false;
        }

        // Initialise email builder
        EmailPopulatingBuilder emailBuilder = EmailBuilder.startingBlank()
                .from(mailerProperties.getFrom().getName(), mailerProperties.getFrom().getAddress())
                .to(mailerProperties.getTo().getName(), mailerProperties.getTo().getAddress())
                .withSubject(mailerProperties.getSubject());

        List<AttachmentResource> attachments = new ArrayList<>();
        // Create email body
        StringBuilder emailContent = new StringBuilder();
        for (InstagramPost post : posts) {
            emailContent.append("############").append(post.getPlayer().getName()).append(" - ").append(post.getPlayerMatchPerformanceStats().getMatch().getDateAsFormattedString()).append("############\n\n");
            emailContent.append(PostHelper.generatePostCaption(instagramPostProperies.getVersion(), post)).append("\n\n");
            emailContent.append("Stat image(s)\n").append(PostHelper.generateS3UrlList(post)).append("\n");
            emailContent.append("Google image search links\n").append(PostHelper.generatePostImageSearchUrl(post)).append("\n\n\n");

            // Add images to attachment - config driven
            if (mailerProperties.isAttachImages()) {
                if (!post.getImagesFileNames().isEmpty()) {
                    for (String fileName : post.getImagesFileNames()) {
                        attachments.add(new AttachmentResource(fileName, new FileDataSource(imageGeneratorProperies.getOutputPath() + fileName)));
                    }
                }
            }
        }
        // Add caption, attachments
        emailBuilder = emailBuilder.withPlainText(emailContent.toString());
        emailBuilder = emailBuilder.withAttachments(attachments);

        // Send email
        try {
            log.info("Attempting to send email");
            Email email = emailBuilder.buildEmail();

            Mailer mailer = MailerBuilder
                    .withSMTPServer("smtp.gmail.com", 587, mailerProperties.getFrom().getAddress(), mailerProperties.getFrom().getPassword())
                    .withTransportStrategy(TransportStrategy.SMTP_TLS)
                    .withSessionTimeout(10 * 1000)
                    .clearEmailValidator()
                    .withDebugLogging(true)
                    .buildMailer();

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
