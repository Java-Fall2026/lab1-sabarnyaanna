package ua.common;

import java.time.LocalDateTime;

public abstract class BaseEntity {

    protected final LocalDateTime createdAt;

    protected BaseEntity() {
        this.createdAt = LocalDateTime.now();
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
