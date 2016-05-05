package com.microsoft.auth;

import java.util.UUID;

public class CallState {
    UUID correlationId;
    CallState(UUID correlationId) {
        this.correlationId = correlationId;
    }
}
