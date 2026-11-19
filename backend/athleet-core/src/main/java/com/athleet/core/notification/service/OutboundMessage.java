package com.athleet.core.notification.service;

/** An email/SMS to one person. The body may contain secrets (links), so senders must never log it in production. */
public record OutboundMessage(String toEmail, String toPhone, String subject, String body) {
}
