package com.grad.book;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 编程课程作业提交与互评系统启动类（通道 2，端口 8081）。
 * 端口由启动参数 --server.port 覆盖，代码里不写死，避免多通道并行时撞车。
 */
@SpringBootApplication
@MapperScan("com.grad.book.mapper")
public class BookApplication {
    public static void main(String[] args) {
        SpringApplication.run(BookApplication.class, args);
    }
}
