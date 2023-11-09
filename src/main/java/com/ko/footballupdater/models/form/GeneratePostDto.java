package com.ko.footballupdater.models.form;

import com.ko.footballupdater.models.Player;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Getter
@Setter
public class GeneratePostDto {

    private HashMap<String, String> availableStatMap;

    private List<String> selectedStats;

}
