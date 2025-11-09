package com.browzwi.webscraper.config;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.scheduling.quartz.SpringBeanJobFactory;

/**
 * Configuration class for setting up Quartz scheduler integration with Spring.
 * This configuration enables Spring-managed beans to be used as Quartz jobs
 * with proper dependency injection.
 *
 * @since 1.0
 */
@Configuration
public class QuartzConfig {

    /**
     * Creates and configures the Quartz SchedulerFactoryBean.
     * This factory bean is configured to use a custom job factory that enables
     * Spring's dependency injection in Quartz job instances.
     *
     * @param applicationContext the Spring application context
     * @return the configured SchedulerFactoryBean
     */
    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(ApplicationContext applicationContext) {
        SchedulerFactoryBean factoryBean = new SchedulerFactoryBean();
        factoryBean.setJobFactory(jobFactory(applicationContext.getAutowireCapableBeanFactory()));
        return factoryBean;
    }

    /**
     * Creates a custom SpringBeanJobFactory that enables dependency injection
     * for Quartz job instances.
     *
     * @param beanFactory the Spring bean factory for autowiring
     * @return a custom SpringBeanJobFactory implementation
     */
    private SpringBeanJobFactory jobFactory(AutowireCapableBeanFactory beanFactory) {
        return new SpringBeanJobFactory() {
            @Override
            protected Object createJobInstance(org.quartz.spi.TriggerFiredBundle bundle) throws Exception {
                Object job = super.createJobInstance(bundle);
                beanFactory.autowireBean(job);
                return job;
            }
        };
    }

    /**
     * Creates and returns the configured Quartz Scheduler instance.
     *
     * @param factoryBean the configured factory bean
     * @return the Quartz Scheduler instance
     * @throws SchedulerException if there's an error creating the scheduler
     */
    @Bean
    public Scheduler scheduler(SchedulerFactoryBean factoryBean) throws SchedulerException {
        return factoryBean.getScheduler();
    }
}
