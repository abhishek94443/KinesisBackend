package com.myapp.kinesis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableJpaAuditing // Enables automatic setting of createdAt/updatedAt fields
@EnableScheduling // Enables @Scheduled for cron jobs (like our "No-Show" checker)
public class KinesisApplication {

    public static void main(String[] args) {
        SpringApplication.run(KinesisApplication.class, args);
    }

}
