package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "audit_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private String action;
    @Column(nullable = false) private String entityType;
    private Long entityId;
    private String userEmail;
    @Column(length = 2000) private String details;

    @Enumerated(EnumType.STRING) private Outcome outcome;
    @Column(nullable = false) private LocalDateTime timestamp;

    @PrePersist
    public void prePersist() { if (this.timestamp == null) this.timestamp = LocalDateTime.now(); }

    public enum Outcome { SUCCESS, FAILURE, DENIED }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String entityType) {
        this.entityType = entityType;
    }

    public Long getEntityId() {
        return entityId;
    }

    public void setEntityId(Long entityId) {
        this.entityId = entityId;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public void setUserEmail(String userEmail) {
        this.userEmail = userEmail;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public Outcome getOutcome() {
        return outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    
    
    // Método estático para obtener el builder
public static Builder builder() {
    return new Builder();
}

// Clase Builder interna
public static class Builder {
    private Long id;
    private String action;
    private String entityType;
    private Long entityId;
    private String userEmail;
    private String details;
    private Outcome outcome;
    private LocalDateTime timestamp;
    
    public Builder id(Long id) {
        this.id = id;
        return this;
    }
    
    public Builder action(String action) {
        this.action = action;
        return this;
    }
    
    public Builder entityType(String entityType) {
        this.entityType = entityType;
        return this;
    }
    
    public Builder entityId(Long entityId) {
        this.entityId = entityId;
        return this;
    }
    
    public Builder userEmail(String userEmail) {
        this.userEmail = userEmail;
        return this;
    }
    
    public Builder details(String details) {
        this.details = details;
        return this;
    }
    
    public Builder outcome(Outcome outcome) {
        this.outcome = outcome;
        return this;
    }
    
    public Builder timestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public AuditLog build() {
        return new AuditLog(this);
    }
}
}
