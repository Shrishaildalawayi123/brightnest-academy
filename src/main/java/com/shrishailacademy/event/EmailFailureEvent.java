package com.shrishailacademy.event;

import org.springframework.context.ApplicationEvent;

public class EmailFailureEvent extends ApplicationEvent {

    private final String to;
    private final String subject;
    private final String cause;

    public EmailFailureEvent(Object source, String to, String subject, String cause) {
        super(source);
        this.to = to;
        this.subject = subject;
        this.cause = cause;
    }

    public String getTo() {
        return to;
    }

    public String getSubject() {
        return subject;
    }

    public String getCause() {
        return cause;
    }
}
