package com.validationgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
public class DevicePackageRabbitConfig {

    private static final Logger log = LoggerFactory.getLogger(DevicePackageRabbitConfig.class);

    @Bean
    public ApplicationRunner rabbitConnectionSettingsLogger(
            DeviceProperties deviceProperties,
            @Value("${spring.rabbitmq.host}") String host,
            @Value("${spring.rabbitmq.port}") int port,
            @Value("${spring.rabbitmq.username}") String username,
            @Value("${spring.rabbitmq.virtual-host}") String virtualHost,
            @Value("${spring.rabbitmq.ssl.enabled:false}") boolean sslEnabled,
            @Value("${app.rabbitmq.device-package-exchange:level4.device.packages}") String exchangeName) {
        return args -> log.info(
                "RabbitMQ settings resolved; host={}, port={}, username={}, virtualHost={}, sslEnabled={}, exchange={}, queue={}",
                host,
                port,
                username,
                virtualHost,
                sslEnabled,
                exchangeName,
                deviceProperties.packageQueueName());
    }

    @Bean
    public Queue devicePackageQueue(DeviceProperties deviceProperties) {
        String queueName = deviceProperties.packageQueueName();
        log.info("Declaring device package queue '{}'", queueName);
        return new Queue(queueName, true);
    }

    @Bean
    public TopicExchange devicePackageExchange(
            @Value("${app.rabbitmq.device-package-exchange:level4.device.packages}") String exchangeName) {
        log.info("Declaring device package exchange '{}'", exchangeName);
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding devicePackageBinding(Queue devicePackageQueue, TopicExchange devicePackageExchange) {
        log.info("Binding device package queue '{}' to exchange '{}' with routing key '{}'",
                devicePackageQueue.getName(), devicePackageExchange.getName(), devicePackageQueue.getName());
        return BindingBuilder.bind(devicePackageQueue)
                .to(devicePackageExchange)
                .with(devicePackageQueue.getName());
    }
}
