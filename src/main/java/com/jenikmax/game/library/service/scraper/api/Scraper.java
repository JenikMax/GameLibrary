package com.jenikmax.game.library.service.scraper.api;

import com.jenikmax.game.library.model.dto.GameDto;

public interface Scraper {

    String getType();

    GameDto scrap(GameDto gameDto);

    GameDto scrap(GameDto gameDto, String url);
}
