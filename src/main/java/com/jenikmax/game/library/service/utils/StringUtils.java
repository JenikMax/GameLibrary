package com.jenikmax.game.library.service.utils;

import org.springframework.stereotype.Component;

/**
 * Утилитарный сервис для работы со строками.
 */
@Component
public class StringUtils {

    /**
     * Заменяет пробелы на неразрывные (&nbsp;).
     */
    public String replaceSpacesWithHtmlEntities(String text) {
        return text.replaceAll(" ", "&nbsp;");
    }
}
