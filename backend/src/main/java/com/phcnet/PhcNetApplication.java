package com.phcnet;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhcNetApplication {

    public static void main(String[] args) {
        SpringApplication.run(PhcNetApplication.class, args);
    }
}
