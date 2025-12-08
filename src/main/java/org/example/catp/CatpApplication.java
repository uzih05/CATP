package org.example.catp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling // 스케줄링 기능
@SpringBootApplication
public class CatpApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatpApplication.class, args);
    }

}