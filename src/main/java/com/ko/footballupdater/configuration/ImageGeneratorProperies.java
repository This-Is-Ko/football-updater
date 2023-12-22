package com.ko.footballupdater.configuration;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "image.generator")
public class ImageGeneratorProperies {

    @NotNull
    private boolean enabled;

    @NotNull
    private String inputPath;

    @NotNull
    private String outputPath;

    @NotNull
    private String genericBaseImageFile;

}
