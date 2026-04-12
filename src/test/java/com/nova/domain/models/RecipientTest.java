package com.nova.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RecipientTest {

    // --- EmailContact ---

    @Test
    void testValidEmailContact() {
        assertDoesNotThrow(() -> new EmailContact("test@example.com"));
    }

    @Test
    void testInvalidEmailContact() {
        assertThrows(IllegalArgumentException.class, () -> new EmailContact("testexample.com"));
    }

    @Test
    void testNullEmailContact() {
        assertThrows(IllegalArgumentException.class, () -> new EmailContact(null));
    }

    // --- SmsContact ---

    @Test
    void testValidSmsContact() {
        assertDoesNotThrow(() -> new SmsContact("+1234567890"));
    }

    @Test
    void testInvalidSmsContact() {
        assertThrows(IllegalArgumentException.class, () -> new SmsContact("invalid-phone"));
    }

    @Test
    void testNullSmsContact() {
        assertThrows(IllegalArgumentException.class, () -> new SmsContact(null));
    }

    // --- PushContact ---

    @Test
    void testValidPushContact() {
        assertDoesNotThrow(() -> new PushContact("fcm_device_token"));
    }

    @Test
    void testBlankPushContact() {
        assertThrows(IllegalArgumentException.class, () -> new PushContact("   "));
    }

    @Test
    void testNullPushContact() {
        assertThrows(IllegalArgumentException.class, () -> new PushContact(null));
    }

    // --- NotificationRequest routing derives channelType from contact ---

    @Test
    void testEmailContact_channelType() {
        assertEquals(ChannelType.EMAIL, new EmailContact("user@example.com").channelType());
    }

    @Test
    void testSmsContact_channelType() {
        assertEquals(ChannelType.SMS, new SmsContact("+1234567890").channelType());
    }

    @Test
    void testPushContact_channelType() {
        assertEquals(ChannelType.PUSH, new PushContact("token").channelType());
    }
}
