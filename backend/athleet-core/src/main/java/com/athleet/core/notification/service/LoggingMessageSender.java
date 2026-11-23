package com.athleet.core.notification.service;

import com.athleet.core.common.security.Masking;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * DEVELOPMENT ONLY: writes messages to the log instead of sending them, so invite and reset links can be used
 * locally. Enabled only with athleet.messaging.provider=log, which application-prod.yml must never set.
 */
@Component
@ConditionalOnProperty(name = "athleet.messaging.provider", havingValue = "log")
public class LoggingMessageSender implements MessageSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageSender.class);

    @PostConstruct
    void warn() {
        log.warn("LoggingMessageSender is active: messages are logged, not sent. Do not use outside local development.");
    }

    @Override
    public void send(OutboundMessage m) {
        log.info("[dev message] to={} subject={}\n{}", m.toEmail() != null ? Masking.email(m.toEmail()) : Masking.phone(m.toPhone()),
                m.subject(), m.body());
    }
}
