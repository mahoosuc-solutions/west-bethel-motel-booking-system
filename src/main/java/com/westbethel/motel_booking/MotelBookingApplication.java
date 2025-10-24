package com.westbethel.motel_booking;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MotelBookingApplication {

    public static void main(String[] args) {
        SpringApplication.run(MotelBookingApplication.class, args);
    }
}
