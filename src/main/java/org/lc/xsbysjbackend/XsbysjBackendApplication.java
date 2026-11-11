package org.lc.xsbysjbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//接口文档地址 http://localhost:8080/doc.html
@MapperScan("org.lc.xsbysjbackend.mapper")
@SpringBootApplication
public class XsbysjBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(XsbysjBackendApplication.class, args);
    }

}
