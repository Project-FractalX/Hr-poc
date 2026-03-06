package org.fractalx.registry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FractalRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FractalRegistryApplication.class, args);
    }
}
