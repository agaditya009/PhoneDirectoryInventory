package com.assignment.phoneinventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhoneInventoryStarter {
    public static void main(String[] args) {
        SpringApplication.run(PhoneInventoryStarter.class, args);
    }
}
