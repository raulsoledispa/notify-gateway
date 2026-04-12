package com.nova.domain.models;

import java.util.regex.Pattern;

public record SmsContact(String phoneNumber) implements RecipientContact {

    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$"); // E.164 format

    public SmsContact {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new IllegalArgumentException("Phone number must not be null or blank");
        }
        if (!PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format: " + phoneNumber);
        }
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.SMS;
    }
}
