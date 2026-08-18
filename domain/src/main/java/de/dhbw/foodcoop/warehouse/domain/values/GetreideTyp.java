package de.dhbw.foodcoop.warehouse.domain.values;

import java.util.Arrays;
import java.util.Optional;

public enum GetreideTyp {

    DINKEL('D'),
    WEIZEN('W'),
    ROGGEN('R'),
    HAFER('H'),
    GERSTE('G'),
    KHORASAN('K');

    private final char code;

    GetreideTyp(char code) {
        this.code = code;
    }

    public char getCode() {
        return code;
    }

    public static Optional<GetreideTyp> fromCode(char code) {
        return Arrays.stream(values())
                .filter(type -> type.code == Character.toUpperCase(code))
                .findFirst();
    }
}