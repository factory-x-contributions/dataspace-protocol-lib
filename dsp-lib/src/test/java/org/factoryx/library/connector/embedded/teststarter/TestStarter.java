package org.factoryx.library.connector.embedded.teststarter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"org.factoryx.library"})
@EnableJpaRepositories(basePackages = {"org.factoryx.library"})
@EntityScan(basePackages = {"org.factoryx.library"})
public class TestStarter {
}
