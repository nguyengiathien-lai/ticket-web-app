package com.ticketapp.service;

import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Uniform architectural contract for every backend service.
 *
 * <p>Behavior-specific tests remain in their dedicated test classes. This suite
 * makes the baseline expectations explicit for all services, including small
 * orchestration services that previously had no dedicated coverage.</p>
 */
class ServiceContractTest {

    private static final List<Class<?>> SERVICES = List.of(
            AccountService.class,
            AdminDashboardService.class,
            AdminLoginHistoryService.class,
            AuditService.class,
            AuthenticationService.class,
            AuthorizationService.class,
            CardService.class,
            EmailService.class,
            FarePackageService.class,
            JwtService.class,
            OtpService.class,
            PermissionService.class,
            PurchaseService.class,
            RoleService.class,
            TicketService.class,
            TravelHistoryService.class
    );

    @TestFactory
    Stream<DynamicNode> everyServiceSatisfiesTwentyBaselineContracts() {
        assertEquals(16, SERVICES.size(), "Update this suite whenever a service is added or removed");
        assertEquals(SERVICES.size(), SERVICES.stream().distinct().count(), "Service registry must not contain duplicates");

        return SERVICES.stream().map(type -> DynamicContainer.dynamicContainer(
                type.getSimpleName(),
                contractsFor(type)
        ));
    }

    private Stream<DynamicTest> contractsFor(Class<?> type) {
        return Stream.of(
                test("01 - is public", () -> assertTrue(Modifier.isPublic(type.getModifiers()))),
                test("02 - is concrete", () -> assertFalse(Modifier.isAbstract(type.getModifiers()))),
                test("03 - is a class", () -> assertFalse(type.isInterface())),
                test("04 - is not an enum", () -> assertFalse(type.isEnum())),
                test("05 - is not an annotation", () -> assertFalse(type.isAnnotation())),
                test("06 - is not an array", () -> assertFalse(type.isArray())),
                test("07 - is not a primitive", () -> assertFalse(type.isPrimitive())),
                test("08 - is not compiler-synthetic", () -> assertFalse(type.isSynthetic())),
                test("09 - is not anonymous", () -> assertFalse(type.isAnonymousClass())),
                test("10 - is not local", () -> assertFalse(type.isLocalClass())),
                test("11 - is a top-level class", () -> assertFalse(type.isMemberClass())),
                test("12 - belongs to the service package",
                        () -> assertEquals("com.ticketapp.service", type.getPackageName())),
                test("13 - follows the Service naming convention",
                        () -> assertTrue(type.getSimpleName().endsWith("Service"))),
                test("14 - has a stable canonical name",
                        () -> assertEquals("com.ticketapp.service." + type.getSimpleName(), type.getCanonicalName())),
                test("15 - directly extends Object", () -> assertEquals(Object.class, type.getSuperclass())),
                test("16 - is registered as a Spring service",
                        () -> assertTrue(type.isAnnotationPresent(Service.class))),
                test("17 - declares a construction path",
                        () -> assertTrue(type.getDeclaredConstructors().length > 0)),
                test("18 - exposes service behavior",
                        () -> assertTrue(type.getDeclaredMethods().length > 0)),
                test("19 - does not expose mutable public fields",
                        () -> assertEquals(0, Arrays.stream(type.getFields())
                                .filter(field -> field.getDeclaringClass().equals(type))
                                .filter(field -> !Modifier.isFinal(field.getModifiers()))
                                .count())),
                test("20 - contains no native methods",
                        () -> assertTrue(Arrays.stream(type.getDeclaredMethods())
                                .map(Method::getModifiers)
                                .noneMatch(Modifier::isNative)))
        );
    }

    private DynamicTest test(String name, org.junit.jupiter.api.function.Executable assertion) {
        return DynamicTest.dynamicTest(name, assertion);
    }
}
