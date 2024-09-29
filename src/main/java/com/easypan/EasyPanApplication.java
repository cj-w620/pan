package com.easypan;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;


@EnableAsync    //开启异步调用
@SpringBootApplication(scanBasePackages = {"com.easypan"})
@EnableTransactionManagement  //事务生效
@EnableScheduling   //开启定时任务
@MapperScan(basePackages = {"com.easypan.mappers"})
public class EasyPanApplication {
    public static void main(String[] args) {
        SpringApplication.run(EasyPanApplication.class,args);
    }
}
