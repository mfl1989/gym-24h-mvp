package com.gym24h.infrastructure.external.iot;

public interface DoorLockClient {

    boolean open(String userId, String requestId);
}
