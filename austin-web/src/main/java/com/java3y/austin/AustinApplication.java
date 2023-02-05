package com.java3y.austin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


/**
 * @author 3y
 */
@SpringBootApplication
public class AustinApplication {
    public static void main(String[] args) {

        /**
         * 如果你需要启动Apollo动态配置
         * 1、启动apollo
         * 2、将application.properties配置文件的 austin.apollo.enabled 改为true
         * 3、下方的property替换真实的ip和port
         */
        System.setProperty("apollo.config-service", "http://192.168.33.10:8090");       //配置apollo的config-service服务的地址和端口
        SpringApplication.run(AustinApplication.class, args);
    }
}
