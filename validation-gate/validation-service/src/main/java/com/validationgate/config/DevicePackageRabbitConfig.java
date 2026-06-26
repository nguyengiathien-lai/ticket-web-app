package com.validationgate.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DevicePackageRabbitConfig {

    @Bean
    public Queue devicePackageQueue(DeviceProperties deviceProperties) {
        return new Queue(deviceProperties.packageQueueName(), true);
    }

    @Bean
    public TopicExchange devicePackageExchange(
            @Value("${app.rabbitmq.device-package-exchange:level4.device.packages}") String exchangeName) {
        return new TopicExchange(exchangeName, true, false);
    }

    @Bean
    public Binding devicePackageBinding(Queue devicePackageQueue, TopicExchange devicePackageExchange) {
        return BindingBuilder.bind(devicePackageQueue)
                .to(devicePackageExchange)
                .with(devicePackageQueue.getName());
    }
}
