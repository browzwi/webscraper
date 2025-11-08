package com.browzwi.officefinder;

import org.springframework.boot.SpringApplication;

public class TestOfficefinderApplication {

	public static void main(String[] args) {
		SpringApplication.from(OfficefinderApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
