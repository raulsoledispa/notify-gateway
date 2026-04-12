package com.nova.domain.models;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SlackContactTest {

    @Test
    void testValidSlackContact() {
        assertDoesNotThrow(() -> new SlackContact("https://hooks.slack.com/services/T00/B00/xxxx"));
    }

    @Test
    void testNullWebhookUrl() {
        assertThrows(IllegalArgumentException.class, () -> new SlackContact(null));
    }

    @Test
    void testBlankWebhookUrl() {
        assertThrows(IllegalArgumentException.class, () -> new SlackContact("   "));
    }

    @Test
    void testInvalidWebhookUrl() {
        assertThrows(IllegalArgumentException.class, () -> new SlackContact("https://example.com/webhook"));
    }

    @Test
    void testSlackContact_channelType() {
        assertEquals(ChannelType.SLACK, new SlackContact("https://hooks.slack.com/services/T00/B00/xxxx").channelType());
    }
}
