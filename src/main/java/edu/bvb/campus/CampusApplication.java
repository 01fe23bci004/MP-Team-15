package edu.bvb.campus;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(exclude = org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration.class)
public class CampusApplication {
    public static void main(String[] args) { SpringApplication.run(CampusApplication.class, args); }
}
