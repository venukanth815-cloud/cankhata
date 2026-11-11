package com.cankhata.service;

import com.cankhata.exception.ValidationException;
import com.cankhata.model.Customer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CustomerServiceTest {

    private static Customer form() {
        Customer c = new Customer();
        c.setName("  Lakshmi ");
        c.setPhone("+91 98765 43210");
        c.setAddress("12, 3rd Street");
        c.setLandmark(" ");
        c.setRouteId(4L);
        return c;
    }

    @Test
    void cleanTrimsAndConverts() {
        Customer c = CustomerService.clean(form(), "38.50", "");
        assertEquals("Lakshmi", c.getName());
        assertEquals("9876543210", c.getPhone());
        assertNull(c.getLandmark());
        assertEquals(3850L, c.getRateOverridePaise().longValue());
        assertEquals(0L, c.getCreditLimitPaise());
        assertEquals(4L, c.getRouteId().longValue());
    }

    @Test
    void blankRateMeansUseCanTypePrice() {
        assertNull(CustomerService.clean(form(), "", "100").getRateOverridePaise());
        assertEquals(10000L, CustomerService.clean(form(), null, "100").getCreditLimitPaise());
    }

    @Test
    void invalidFieldsAreRejected() {
        Customer bad = form();
        bad.setPhone("123");
        assertThrows(ValidationException.class, () -> CustomerService.clean(bad, "", ""));
        Customer noAddr = form();
        noAddr.setAddress(" ");
        assertThrows(ValidationException.class, () -> CustomerService.clean(noAddr, "", ""));
        assertThrows(ValidationException.class, () -> CustomerService.clean(form(), "0", ""));
    }
}
