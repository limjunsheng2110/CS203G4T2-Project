package com.cs205.tariffg4t2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;

@SpringBootApplication
public class Tariffg4t2Application implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(Tariffg4t2Application.class, args);
    }


    @Override
    public void run(String... args) throws Exception {
        System.out.println("Testing");


    }

}
