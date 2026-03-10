package com.reto.ms_order_loading;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class MsOrderLoadingApplication {

	public static void main(String[] args) {
		SpringApplication.run(MsOrderLoadingApplication.class, args);
	}

}
