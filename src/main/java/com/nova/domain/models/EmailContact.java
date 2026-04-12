package com.nova.domain.models;

import java.util.regex.Pattern;

public record EmailContact(String email) implements RecipientContact {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@(.+)$");

    public EmailContact {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email must not be null or blank");
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format: " + email);
        }
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.EMAIL;
    }
}
