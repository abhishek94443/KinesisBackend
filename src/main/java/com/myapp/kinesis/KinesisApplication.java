package com.myapp.kinesis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The main entry point for the Kinesis SaaS Backend.
 *
 * @SpringBootApplication is a master annotation that includes:
 * - @Configuration: Tags this as a Spring configuration class.
 * - @EnableAutoConfiguration: Tells Spring Boot to automatically configure
 * beans based on our pom.xml dependencies (like Tomcat, JPA, Security).
 * - @ComponentScan: Tells Spring to scan this package (and all sub-packages)
 * for our @Component, @Service, @Repository, and @RestController beans.
 */
@SpringBootApplication
public class KinesisApplication {

    public static void main(String[] args) {
        SpringApplication.run(KinesisApplication.class, args);
    }

}