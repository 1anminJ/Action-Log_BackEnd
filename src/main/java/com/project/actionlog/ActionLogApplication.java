package com.project.actionlog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
public class ActionLogApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActionLogApplication.class, args);
    }

}
