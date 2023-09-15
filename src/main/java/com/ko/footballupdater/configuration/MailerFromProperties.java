package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Component
@ConfigurationProperties(prefix = "from")
public class MailerFromProperties {

    @NotNull
    private String name;

    @NotNull
    private String address;

    @NotNull
    private String password;

    public void setName(String name) {
        this.name = name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}