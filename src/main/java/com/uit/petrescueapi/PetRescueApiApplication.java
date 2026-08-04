package com.uit.petrescueapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetRescueApiApplication {

    public static void main(String[] args) {

        SpringApplication.run(PetRescueApiApplication.class, args);
    }

}
