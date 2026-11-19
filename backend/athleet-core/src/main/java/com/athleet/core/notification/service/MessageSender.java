package com.athleet.core.notification.service;

/** Strategy for delivering messages (log-only in dev; email/SMS/WhatsApp providers later). */
public interface MessageSender {
    void send(OutboundMessage message);
}
