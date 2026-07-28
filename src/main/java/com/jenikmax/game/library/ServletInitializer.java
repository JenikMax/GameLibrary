package com.jenikmax.game.library;

import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * Инициализатор сервлета для развёртывания WAR-файла на внешнем Tomcat.
 * Позволяет запускать приложение не только через встроенный Tomcat, но и как WAR.
 */
public class ServletInitializer extends SpringBootServletInitializer {

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(GameLibraryAppApplication.class);
	}

}
