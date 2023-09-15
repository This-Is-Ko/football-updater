package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "mailer")
public class MailerProperties {

    @NotNull
    private String subject;
    private MailerFromProperties from;
    private MailerToProperties to;

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public MailerFromProperties getFrom() {
        return from;
    }

    public void setFrom(MailerFromProperties from) {
        this.from = from;
    }

    public MailerToProperties getTo() {
        return to;
    }

    public void setTo(MailerToProperties to) {
        this.to = to;
    }
}
