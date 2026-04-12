package com.nova.infrastructure.config;

/**
 * Configuration value object for the Twilio SMS provider.
 *
 * @param accountSid      The Twilio Account SID. Mandatory.
 * @param authToken       The Twilio Auth Token. Mandatory.
 * @param fromPhoneNumber The E.164-formatted phone number to send messages from. Mandatory.
 */
public record TwilioConfig(String accountSid, String authToken, String fromPhoneNumber) {
    public TwilioConfig {
        if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
            throw new IllegalArgumentException("Twilio credentials cannot be empty");
        }
        if (fromPhoneNumber == null || fromPhoneNumber.isBlank()) {
            throw new IllegalArgumentException("Twilio fromPhoneNumber cannot be empty");
        }
    }
}
