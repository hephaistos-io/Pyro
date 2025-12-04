package io.hephaistos.pyro;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "io.hephaistos.pyro.data.repository")
@EntityScan("io.hephaistos.pyro.data")
@EnableTransactionManagement
public class PyroApplication {

    public static void main(String[] args) {
        SpringApplication.run(PyroApplication.class, args);
    }

}
