package com.fullstack.demo.starter;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.fullstack.demo.infrastructure.mapper")
@SpringBootApplication(scanBasePackages = "com.fullstack.demo")
public class DemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
        System.out.println("(♥◠‿◠)ノ゙  Demo 启动成功   ლ(´ڡ`ლ)゙");
    }
}
