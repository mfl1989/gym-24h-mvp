package com.gym24h.infrastructure.external.doorlock;

import com.gym24h.application.outbound.DoorLockClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "door-lock.mock", havingValue = "true")
public class LocalMockDoorLockClient implements DoorLockClient {

    private static final Logger log = LoggerFactory.getLogger(LocalMockDoorLockClient.class);

    @Override
    public void unlock(String deviceId) {
        log.info("local mock door unlock accepted. deviceId={}", deviceId);
    }
}