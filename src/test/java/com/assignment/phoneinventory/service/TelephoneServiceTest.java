package com.assignment.phoneinventory.service;

import com.assignment.phoneinventory.dao.TelephoneNumberDao;
import com.assignment.phoneinventory.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
class TelephoneServiceTest {

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

