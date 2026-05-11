package com.gj.mall;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * GJ Mall Shop 启动类
 */
@SpringBootApplication(scanBasePackages = "com.gj.mall")
@MapperScan("com.gj.mall.**.mapper")
@EnableMongoRepositories(basePackages = "com.gj.mall.**.repo")
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
public class MallApplication {

    public static void main(String[] args) {
        SpringApplication.run(MallApplication.class, args);
        System.out.println("======================================");
        System.out.println(" gj-mall-shop 启动成功");
        System.out.println(" 接口文档: http://localhost:8080/doc.html");
        System.out.println(" 健康检查: http://localhost:8080/api/v1/ping");
        System.out.println("======================================");
    }
}
