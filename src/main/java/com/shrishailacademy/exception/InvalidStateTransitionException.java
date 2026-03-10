package com.shrishailacademy.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an invalid state transition is attempted (e.g., confirming a
 * FAILED payment).
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class InvalidStateTransitionException extends BusinessException {

    private final String currentState;
    private final String targetState;

    public InvalidStateTransitionException(String entity, String currentState, String targetState) {
        super("INVALID_STATE",
                String.format("Cannot transition %s from %s to %s", entity, currentState, targetState));
        this.currentState = currentState;
        this.targetState = targetState;
    }

    public String getCurrentState() {
        return currentState;
    }

    public String getTargetState() {
        return targetState;
    }
}
