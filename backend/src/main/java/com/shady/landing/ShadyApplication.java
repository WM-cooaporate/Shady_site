package com.shady.landing;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class ShadyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ShadyApplication.class, args);
    }
}
