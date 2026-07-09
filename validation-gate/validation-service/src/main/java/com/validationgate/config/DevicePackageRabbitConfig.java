package com.validationgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Declarable;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.boot.ApplicationRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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
                "RabbitMQ settings resolved; host={}, port={}, username={}, virtualHost={}, sslEnabled={}, exchange={}, queues={}",
                host,
                port,
                username,
                virtualHost,
                sslEnabled,
                exchangeName,
                Arrays.toString(deviceProperties.packageQueueNames()));
    }

    @Bean("devicePackageQueueNames")
    public String[] devicePackageQueueNames(DeviceProperties deviceProperties) {
        return deviceProperties.packageQueueNames();
    }

    @Bean
    public TopicExchange devicePackageExchange(
            @Value("${app.rabbitmq.device-package-exchange:level4.device.packages}") String exchangeName) {
        log.info("Declaring device package exchange '{}'", exchangeName);
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Declarables devicePackageBindings(
            DeviceProperties deviceProperties,
            TopicExchange devicePackageExchange) {
        List<Declarable> declarables = new ArrayList<>();
        for (DeviceProperties.DeviceRegistration registration : deviceProperties.devices()) {
            String queueName = "device." + registration.deviceCode();
            String bindingKey = "device." + registration.stationCode();
            Queue queue = new Queue(queueName, true);
            Binding binding = BindingBuilder.bind(queue).to(devicePackageExchange).with(bindingKey);
            declarables.add(queue);
            declarables.add(binding);
            log.info("Declaring device package queue '{}' and binding it to exchange '{}' with key '{}'",
                    queueName, devicePackageExchange.getName(), bindingKey);
        }
        return new Declarables(declarables);
    }
}
