package com.gym24h.domain.model.common;

import java.util.Objects;

public abstract class BaseEntity<ID> {

    private final ID id;
    private long version;
    private boolean deleted;

    protected BaseEntity(ID id, long version, boolean deleted) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.version = version;
        this.deleted = deleted;
    }

    public ID getId() {
        return id;
    }

    public long getVersion() {
        return version;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void markDeleted() {
        this.deleted = true;
    }

    public void syncVersion(long version) {
        this.version = version;
    }
}
