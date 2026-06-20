package com.mobai.mopicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@MapperScan("com.mobai.mopicturebackend.mapper")
@EnableAspectJAutoProxy(exposeProxy = true)
public class MoPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(MoPictureBackendApplication.class, args);
        System.out.println("智能云图库项目已启动");
    }

}