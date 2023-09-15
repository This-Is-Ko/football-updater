package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "mailer")
public class MailerProperties {

    @NotNull
    private String subject;
    private MailerFromProperties from;
    private MailerToProperties to;

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setFrom(MailerFromProperties from) {
        this.from = from;
    }

    public void setTo(MailerToProperties to) {
        this.to = to;
    }
}
