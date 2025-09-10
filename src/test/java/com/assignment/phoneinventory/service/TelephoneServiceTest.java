package com.assignment.phoneinventory.service;

import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@Testcontainers
class TelephoneServiceTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8")
            .withDatabaseName("phones")
            .withUsername("app")
            .withPassword("app");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);
    }

    @Autowired
    private TelephoneService service;

    @Autowired
    private TelephoneNumberDao dao;

    @Test
    void activatingWithDifferentUserFails() {
        String number = "5551234";
        dao.upsertNumber(number, "1", "2");
        service.reserve(number, "userA", Duration.ofMinutes(15));
        service.allocate(number, "userA");

        assertThrows(BusinessRuleViolationException.class,
                () -> service.activate(number, "userB"));
    }
}

