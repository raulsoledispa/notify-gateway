package com.nova.domain.models;

public sealed interface RecipientContact permits EmailContact, SmsContact, PushContact {
    ChannelType channelType();
}
