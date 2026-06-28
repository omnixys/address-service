package com.omnixys.address;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;

@SpringBootTest(classes = AddressApplicationTest.TestApp.class)
class AddressApplicationTest {

    @Test
    void contextLoads() {
    }

    @SpringBootConfiguration
    @ComponentScan("com.omnixys.address.config")
    static class TestApp {
    }
}
