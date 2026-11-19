package com.athleet.core.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** Used until a real email/SMS provider is configured: drops messages without logging their content. */
@Component
@ConditionalOnProperty(name = "athleet.messaging.provider", havingValue = "none", matchIfMissing = true)
public class DisabledMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(DisabledMessageSender.class);

    @Override
    public void send(OutboundMessage message) {
        log.warn("Messaging provider is not configured; a message was not sent.");
    }
}
