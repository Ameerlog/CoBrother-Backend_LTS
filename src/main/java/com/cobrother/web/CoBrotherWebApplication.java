package com.cobrother.web;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class CoBrotherWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoBrotherWebApplication.class, args);
    }

}
