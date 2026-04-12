package com.nova.domain.models;

import java.util.Optional;
import java.util.regex.Pattern;

public record Recipient(
        String email,
        String phoneNumber,
        String pushToken
) {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[1-9]\\d{1,14}$"); // E.164 format

    public Recipient {
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }
        if (phoneNumber != null && !PHONE_PATTERN.matcher(phoneNumber).matches()) {
            throw new IllegalArgumentException("Invalid phone number format");
        }
    }

    public Optional<String> getEmail() {
        return Optional.ofNullable(email);
    }

    public Optional<String> getPhoneNumber() {
        return Optional.ofNullable(phoneNumber);
    }

    public Optional<String> getPushToken() {
        return Optional.ofNullable(pushToken);
    }
}
