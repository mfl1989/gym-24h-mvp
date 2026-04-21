package com.gym24h.domain.model.user;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Objects;

@Getter
@ToString
@EqualsAndHashCode(of = "id")
public class User {

    private final UserId id;
    private final String lineUserId;
    private final String phoneNumber;
    private final String displayName;
    private String membershipStatus;
    private Instant createdAt;
    private Instant updatedAt;
    private int version;
    private boolean deleted;
    private final String subChar1;
    private final String subChar2;
    private final String subChar3;
    private final String subChar4;
    private final String subChar5;
    private final String subChar6;
    private final String subChar7;
    private final String subChar8;
    private final String subChar9;
    private final String subChar10;

    @Builder(toBuilder = true)
    public User(
            UserId id,
            String lineUserId,
            String phoneNumber,
            String displayName,
            String membershipStatus,
            Instant createdAt,
            Instant updatedAt,
            int version,
            boolean deleted,
            String subChar1,
            String subChar2,
            String subChar3,
            String subChar4,
            String subChar5,
            String subChar6,
            String subChar7,
            String subChar8,
            String subChar9,
            String subChar10
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.lineUserId = Objects.requireNonNull(lineUserId, "lineUserId must not be null");
        this.phoneNumber = phoneNumber;
        this.displayName = displayName;
        this.membershipStatus = Objects.requireNonNull(membershipStatus, "membershipStatus must not be null");
        this.createdAt = Objects.requireNonNull(createdAt, "createdAt must not be null");
        this.updatedAt = Objects.requireNonNull(updatedAt, "updatedAt must not be null");
        this.version = version;
        this.deleted = deleted;
        this.subChar1 = subChar1;
        this.subChar2 = subChar2;
        this.subChar3 = subChar3;
        this.subChar4 = subChar4;
        this.subChar5 = subChar5;
        this.subChar6 = subChar6;
        this.subChar7 = subChar7;
        this.subChar8 = subChar8;
        this.subChar9 = subChar9;
        this.subChar10 = subChar10;
    }

    public User(UserId id, String lineUserId, boolean active, long version, boolean deleted) {
        this(
                id,
                lineUserId,
                null,
                null,
                active ? "ACTIVE" : "CANCELED",
                Instant.now(),
                Instant.now(),
                Math.toIntExact(version),
                deleted,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static User create(String lineUserId) {
        Instant now = Instant.now();
        return User.builder()
                .id(UserId.newId())
                .lineUserId(lineUserId)
                .membershipStatus("ACTIVE")
                .createdAt(now)
                .updatedAt(now)
                .version(0)
                .deleted(false)
                .build();
    }

    public boolean isActive() {
        return "ACTIVE".equals(membershipStatus) && !deleted;
    }

    public void deactivate() {
        this.membershipStatus = "CANCELED";
        touch();
    }

    public void markDeleted() {
        this.deleted = true;
        touch();
    }

    public void syncVersion(int version) {
        this.version = version;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
