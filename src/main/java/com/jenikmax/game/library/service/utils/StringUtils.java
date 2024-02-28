package com.jenikmax.game.library.service.utils;

import org.springframework.stereotype.Component;

@Component
public class StringUtils {

    public String replaceSpacesWithHtmlEntities(String text) {
        return text.replaceAll(" ", "&nbsp;");
    }
}
