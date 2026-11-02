package br.edu.ifro.ji;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class JiApplication {

    public static void main(String[] args) {
        SpringApplication.run(JiApplication.class, args);
    }
}
