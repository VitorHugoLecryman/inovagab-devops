package br.com.fiap.inovagab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class InovaGabApplication {

    public static void main(String[] args) {
        SpringApplication.run(InovaGabApplication.class, args);
    }
}
