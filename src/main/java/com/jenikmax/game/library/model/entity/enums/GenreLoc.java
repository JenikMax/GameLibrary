package com.jenikmax.game.library.model.entity.enums;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.Map;

@Component
@ConfigurationProperties("enum")
public class GenreLoc {

    private final Map<Genre, String> genre = new EnumMap<>(Genre.class);

    public Map<Genre, String> getStatus() {
        return genre;
    }
}
