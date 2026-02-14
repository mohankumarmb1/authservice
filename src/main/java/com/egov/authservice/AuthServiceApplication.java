package com.egov.authservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication
@ServletComponentScan
public class AuthServiceApplication
{

    public static void main(String[] args)
    {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
