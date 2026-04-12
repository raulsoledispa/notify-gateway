package com.nova.domain.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RecipientTest {
    @Test
    void testValidEmail() {
        assertDoesNotThrow(() -> new Recipient("test@example.com", null, null));
    }

    @Test
    void testInvalidEmail() {
        assertThrows(IllegalArgumentException.class, () -> new Recipient("testexample.com", null, null));
    }

    @Test
    void testValidPhoneNumber() {
        assertDoesNotThrow(() -> new Recipient(null, "+1234567890", null));
    }

    @Test
    void testInvalidPhoneNumber() {
        assertThrows(IllegalArgumentException.class, () -> new Recipient(null, "invalid-phone", null));
    }
}
