package com.nova.domain.models;

public record PushContact(String pushToken) implements RecipientContact {

    public PushContact {
        if (pushToken == null || pushToken.isBlank()) {
            throw new IllegalArgumentException("Push token must not be null or blank");
        }
    }

    @Override
    public ChannelType channelType() {
        return ChannelType.PUSH;
    }
}
