package ru.hh.nab.hibernate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.integrator.spi.Integrator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.orm.hibernate5.HibernateTransactionManager;

import javax.sql.DataSource;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Stream;

import org.springframework.transaction.annotation.EnableTransactionManagement;
import ru.hh.nab.hibernate.transaction.DataSourceContextTransactionManager;
import ru.hh.nab.hibernate.transaction.ExecuteOnDataSourceAspect;
import ru.hh.nab.hibernate.transaction.TransactionalScope;

import static java.util.stream.Collectors.toMap;

@Configuration
@EnableTransactionManagement(order = 0)
@EnableAspectJAutoProxy
public class NabHibernateCommonConfig {

  @Bean
  DataSourceContextTransactionManager transactionManager(SessionFactory sessionFactory, DataSource dataSource) {
    HibernateTransactionManager simpleTransactionManager = new HibernateTransactionManager(sessionFactory);
    simpleTransactionManager.setDataSource(dataSource);
    return new DataSourceContextTransactionManager(simpleTransactionManager);
  }

  @Bean
  ExecuteOnDataSourceAspect executeOnDataSourceAspect(ApplicationContext applicationContext) {
    var txManagers
      = Stream.of(applicationContext.getBeanNamesForType(DataSourceContextTransactionManager.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, DataSourceContextTransactionManager.class)));
    var sessionFactories
        = Stream.of(applicationContext.getBeanNamesForType(SessionFactory.class))
        .collect(toMap(Function.identity(), beanName -> applicationContext.getBean(beanName, SessionFactory.class)));
    return new ExecuteOnDataSourceAspect(txManagers, sessionFactories);
  }

  @Bean
  NabSessionFactoryBean sessionFactory(DataSource dataSource, Properties hibernateProperties,
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder, MappingConfig mappingConfig,
    Optional<Collection<NabSessionFactoryBean.ServiceSupplier<?>>> serviceSuppliers,
    Optional<Collection<NabSessionFactoryBean.SessionFactoryCreationHandler>> sessionFactoryCreationHandlers) {
    NabSessionFactoryBean sessionFactoryBean = new NabSessionFactoryBean(dataSource, hibernateProperties, bootstrapServiceRegistryBuilder,
      serviceSuppliers.orElseGet(ArrayList::new), sessionFactoryCreationHandlers.orElseGet(ArrayList::new));
    sessionFactoryBean.setDataSource(dataSource);
    sessionFactoryBean.setAnnotatedClasses(mappingConfig.getAnnotatedClasses());
    sessionFactoryBean.setPackagesToScan(mappingConfig.getPackagesToScan());
    sessionFactoryBean.setHibernateProperties(hibernateProperties);
    return sessionFactoryBean;
  }

  @Bean
  BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder(Optional<Collection<Integrator>> integratorsOptional) {
    BootstrapServiceRegistryBuilder bootstrapServiceRegistryBuilder = new BootstrapServiceRegistryBuilder();
    integratorsOptional.ifPresent(integrators -> integrators.forEach(bootstrapServiceRegistryBuilder::applyIntegrator));
    return bootstrapServiceRegistryBuilder;
  }

  @Bean
  NabSessionFactoryBean.ServiceSupplier<?> nabSessionFactoryBuilderServiceSupplier() {
    return new NabSessionFactoryBean.ServiceSupplier<NabSessionFactoryBuilderFactory.BuilderService>() {
      @Override
      public Class<NabSessionFactoryBuilderFactory.BuilderService> getClazz() {
        return NabSessionFactoryBuilderFactory.BuilderService.class;
      }

      @Override
      public NabSessionFactoryBuilderFactory.BuilderService get() {
        return new NabSessionFactoryBuilderFactory.BuilderService();
      }
    };
  }

  @Bean
  TransactionalScope transactionalScope() {
    return new TransactionalScope();
  }
}
