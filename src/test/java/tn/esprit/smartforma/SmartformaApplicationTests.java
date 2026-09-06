package tn.esprit.smartforma;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Spring Boot application context integration test.
 *
 * @Disabled because this test loads the full Spring context which requires
 * a running MySQL database (smartforma_db).
 *
 * To enable: create the database with 'CREATE DATABASE smartforma_db;' in XAMPP,
 * then remove the @Disabled annotation.
 *
 * Business logic is tested without a DB in InscriptionServiceTest (Mockito).
 */
@SpringBootTest
class SmartformaApplicationTests {

    @Test
    void contextLoads() {
        // Will pass once the database exists and the app starts successfully
    }

}
