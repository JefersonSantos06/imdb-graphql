package com.jsantos.imdbgraphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class SimpleWraperApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimpleWraperApplication.class, args);
    }

}
