package com.gym24h.infrastructure.persistence.entity;

import lombok.Getter;
import lombok.Setter;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "users")
public class UserEntity {

    @Id
    private UUID id;

    @Column(name = "line_user_id", nullable = false, unique = true)
    private String lineUserId;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "membership_status", nullable = false)
    private String membershipStatus;

    @Version
    private int version;

    @Column(name = "is_deleted", nullable = false)
    private boolean deleted;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sub_char1")
    private String subChar1;

    @Column(name = "sub_char2")
    private String subChar2;

    @Column(name = "sub_char3")
    private String subChar3;

    @Column(name = "sub_char4")
    private String subChar4;

    @Column(name = "sub_char5")
    private String subChar5;

    @Column(name = "sub_char6")
    private String subChar6;

    @Column(name = "sub_char7")
    private String subChar7;

    @Column(name = "sub_char8")
    private String subChar8;

    @Column(name = "sub_char9")
    private String subChar9;

    @Column(name = "sub_char10")
    private String subChar10;

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        if (this.updatedAt == null) {
            this.updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
