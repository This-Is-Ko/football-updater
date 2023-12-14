package com.ko.footballupdater.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.Collections.emptyList;

@Converter
public class StringArrayListConverter implements AttributeConverter<ArrayList<String>, String> {
    private static final String SPLIT_CHAR = ";";

    @Override
    public String convertToDatabaseColumn(ArrayList<String> stringList) {
        return stringList != null ? String.join(SPLIT_CHAR, stringList) : "";
    }

    @Override
    public ArrayList<String> convertToEntityAttribute(String string) {
        return string != null ? new ArrayList<>(Arrays.asList(string.split(SPLIT_CHAR))) : new ArrayList<>();
    }
}