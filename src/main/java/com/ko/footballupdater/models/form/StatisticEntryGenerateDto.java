package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatisticEntryGenerateDto {

    private String name;
    private String value;
    private boolean selected;

    public StatisticEntryGenerateDto() {
    }

    public StatisticEntryGenerateDto(String name, String value, boolean selected) {
        this.name = name;
        this.value = value;
        this.selected = selected;
    }
}
