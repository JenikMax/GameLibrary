package com.jenikmax.game.library.service.scraper.api;

import com.jenikmax.game.library.model.dto.GameDto;

/**
 * Интерфейс скрапера — компонента для извлечения метаданных игры
 * из внешних источников (веб-сайтов, API).
 * Каждый скрапер реализует несколько вариантов поиска: по DTO, URL или ScrapInfo.
 */
public interface Scraper {

    String getType();

    GameDto scrap(GameDto gameDto);

    GameDto scrap(GameDto gameDto, String url);

    GameDto scrap(GameDto gameDto, ScrapInfo scrapInfo);
}
