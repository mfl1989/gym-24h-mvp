package com.gym24h.infrastructure.external.iot;

import org.springframework.stereotype.Component;

@Component
public class DoorLockClientImpl implements DoorLockClient {

    @Override
    public boolean open(String userId, String requestId) {
        return true;
    }
}
