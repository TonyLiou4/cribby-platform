package com.santosh.roommate_marketplace_api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class RoommateMarketplaceApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(RoommateMarketplaceApiApplication.class, args);
	}

}
