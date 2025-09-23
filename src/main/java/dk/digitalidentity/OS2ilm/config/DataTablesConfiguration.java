package dk.digitalidentity.OS2ilm.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.datatables.repository.DataTablesRepositoryFactoryBean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@EnableJpaRepositories(repositoryFactoryBeanClass = DataTablesRepositoryFactoryBean.class, basePackages = "dk.digitalidentity.OS2ilm.model.datatable.dao")
public class DataTablesConfiguration {
}
