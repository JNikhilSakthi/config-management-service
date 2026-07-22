package com.medha.configmanagementservice.configserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

/**
 * Entry point for the Config Server.
 *
 * {@code @EnableConfigServer} turns this plain Spring Boot application into a
 * fully-fledged Spring Cloud Config Server: it exposes the standard
 * {@code /{application}/{profile}[/{label}]} HTTP contract (and friends such as
 * {@code /{application}-{profile}.yml}) that config-client applications
 * (order-service, product-service) poll at startup to resolve their
 * environment-specific properties.
 *
 * The actual property source is not defined here — it is delegated entirely to
 * {@code application.yml}, which activates the "native" profile and points at the
 * bundled {@code config-repo/} folder. Swapping to a real Git-backed repository later
 * only requires changing that one block of configuration; no Java code changes needed.
 */
@SpringBootApplication
@EnableConfigServer
public class ConfigServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConfigServerApplication.class, args);
    }
}
