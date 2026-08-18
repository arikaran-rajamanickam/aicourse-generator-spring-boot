package com;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
//@ComponentScan(basePackages = {"com.aicourse", "com.leaderboard"})
public class AiCourseGeneratorApplication {

	public static void main(String[] args) {
		SpringApplication.run(AiCourseGeneratorApplication.class, args);
	}

}
