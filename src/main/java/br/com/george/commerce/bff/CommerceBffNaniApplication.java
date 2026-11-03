package br.com.george.commerce.bff;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class CommerceBffNaniApplication {

    public static void main(String[] args) {
        SpringApplication.run(CommerceBffNaniApplication.class, args);
    }
}
