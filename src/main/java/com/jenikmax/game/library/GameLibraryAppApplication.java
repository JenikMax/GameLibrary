package com.jenikmax.game.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Точка входа в Spring Boot приложение GameLibrary.
 * Система управления библиотекой игр с AI-функциями (семантический поиск, перевод, авто-тегирование).
 */
@SpringBootApplication
@EnableScheduling // Включает планировщик задач (@Scheduled)
public class GameLibraryAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameLibraryAppApplication.class, args);
	}

}
