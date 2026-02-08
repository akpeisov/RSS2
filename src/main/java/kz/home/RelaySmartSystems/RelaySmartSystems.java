package kz.home.RelaySmartSystems;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

//@EnableJpaRepositories(basePackages = "kz.home")
@EnableJpaAuditing
@EnableScheduling
@EntityScan(basePackages = {"kz.home.RelaySmartSystems.model", "kz.home.RelaySmartSystems.model.entity.relaycontroller"})
//@SpringBootApplication(exclude = {SecurityAutoConfiguration.class })
@SpringBootApplication
public class RelaySmartSystems {

	public static void main(String[] args) {
		SpringApplication.run(RelaySmartSystems.class, args);
	}

}
