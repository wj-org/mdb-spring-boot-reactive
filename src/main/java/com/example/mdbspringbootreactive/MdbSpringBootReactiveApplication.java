package com.example.mdbspringbootreactive;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@SpringBootApplication
@EnableMongoRepositories
public class MdbSpringBootReactiveApplication {

	public static void main(String[] args) {
		SpringApplication.run(MdbSpringBootReactiveApplication.class, args);
	}

}
