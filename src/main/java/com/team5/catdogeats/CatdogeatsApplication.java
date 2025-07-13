package com.team5.catdogeats;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
@EnableBatchProcessing
@EnableRetry
public class CatdogeatsApplication {

	public static void main(String[] args) {
		SpringApplication.run(CatdogeatsApplication.class, args);
	}

}
