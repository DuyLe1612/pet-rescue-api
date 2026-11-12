package com.uit.petrescueapi;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PetRescueApiApplication {

    public static void main(String[] args) {
        // Before anything reads a clock or opens a connection: the host's zone must not matter.
        com.uit.petrescueapi.application.support.AppTime.pinJvmToUtc();
        SpringApplication.run(PetRescueApiApplication.class, args);
    }

}
