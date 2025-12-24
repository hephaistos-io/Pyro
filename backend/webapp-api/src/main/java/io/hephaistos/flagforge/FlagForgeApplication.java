package io.hephaistos.flagforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication
@EnableJpaRepositories(basePackages = "io.hephaistos.flagforge.data.repository")
@EntityScan(basePackages = {"io.hephaistos.flagforge.common.data"})
@EnableTransactionManagement
@EnableScheduling
public class FlagForgeApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlagForgeApplication.class, args);
    }

}
