package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "from")
public class MailerFromProperties {

    @NotNull
    private String name;

    @NotNull
    private String address;

    @NotNull
    private String password;

}
