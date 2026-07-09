package com.validationgate.config;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Declarables;
import org.springframework.amqp.core.TopicExchange;

import static org.assertj.core.api.Assertions.assertThat;

class DevicePackageRabbitConfigTest {

    @Test
    void bindsEachDeviceQueueUsingItsStationRoutingKey() {
        DeviceProperties properties = new DeviceProperties(
                null,
                null,
                "HN_2A_01:DEV_HN2A01_ENT_01,HN_2A_01:DEV_HN2A01_EXT_01");
        TopicExchange exchange = new TopicExchange("level4.device.packages");

        Declarables declarables = new DevicePackageRabbitConfig()
                .devicePackageBindings(properties, exchange);

        assertThat(declarables.getDeclarables().stream()
                .filter(Binding.class::isInstance)
                .map(Binding.class::cast))
                .extracting(Binding::getDestination, Binding::getRoutingKey)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(
                                "device.DEV_HN2A01_ENT_01", "device.HN_2A_01"),
                        org.assertj.core.groups.Tuple.tuple(
                                "device.DEV_HN2A01_EXT_01", "device.HN_2A_01"));
    }
}
