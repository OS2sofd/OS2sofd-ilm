package dk.digitalidentity.OS2ilm;

import org.springframework.boot.devtools.restart.RestartScope;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestContainerConfiguration {

    @Bean
    @RestartScope
    @ServiceConnection
    public MariaDBContainer<?> mySQLContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:10.11"));
    }
}
