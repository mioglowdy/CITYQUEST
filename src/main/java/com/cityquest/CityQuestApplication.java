package com.cityquest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.mybatis.spring.annotation.MapperScan;

/**
 * CityQuest 应用启动类
 */
@SpringBootApplication
@MapperScan("com.cityquest.mapper")
@ComponentScan(basePackages = "com.cityquest")
public class CityQuestApplication {

    public static void main(String[] args) {
        SpringApplication.run(CityQuestApplication.class, args);
    }

}