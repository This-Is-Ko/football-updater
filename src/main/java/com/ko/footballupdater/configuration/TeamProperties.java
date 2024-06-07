package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "team")
public class TeamProperties {

    @NotNull
    private ArrayList<String> nameSuffixesToRemove;

}
