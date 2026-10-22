package dk.digitalidentity.OS2ilm.config;

import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;

@Configuration
@EnableJpaAuditing
@EntityScan("dk.digitalidentity")
@EnableJpaRepositories(basePackages = {"dk.digitalidentity.OS2ilm.model.dao", "dk.digitalidentity.emailService.dao"}, repositoryFactoryBeanClass = JpaRepositoryFactoryBean.class)
public class JPAConfiguration {

}
