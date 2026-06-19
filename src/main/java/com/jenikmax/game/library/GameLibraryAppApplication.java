package com.jenikmax.game.library;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class GameLibraryAppApplication {

	public static void main(String[] args) {
		SpringApplication.run(GameLibraryAppApplication.class, args);
	}

}
