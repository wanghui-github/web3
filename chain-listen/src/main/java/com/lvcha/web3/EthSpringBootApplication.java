package com.lvcha.web3;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EthSpringBootApplication {
    public static void main(String[] args) {
        SpringApplication.run(EthSpringBootApplication.class, args);
    }
}
