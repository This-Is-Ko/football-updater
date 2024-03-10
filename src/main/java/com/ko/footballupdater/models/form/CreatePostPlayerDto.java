package com.ko.footballupdater.models.form;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreatePostPlayerDto {

    private Integer id;
    private String name;

    public CreatePostPlayerDto(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
