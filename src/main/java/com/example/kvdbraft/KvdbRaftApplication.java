package com.example.kvdbraft;

import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableDubbo
public class KvdbRaftApplication {

	public static void main(String[] args) {
		SpringApplication.run(KvdbRaftApplication.class, args);
	}

	@PostConstruct
	public void init() {
		// 在这里执行初始化操作
		System.out.println(111);
	}
}
