package com.aicourse.ai.model;

import com.aicourse.ai.AiWorkload;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_llm_routes")
public class AiLlmRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "workload", nullable = false, unique = true, length = 40)
    private AiWorkload workload;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "provider_id", nullable = false)
    private AiLlmProvider provider;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AiWorkload getWorkload() {
        return workload;
    }

    public void setWorkload(AiWorkload workload) {
        this.workload = workload;
    }

    public AiLlmProvider getProvider() {
        return provider;
    }

    public void setProvider(AiLlmProvider provider) {
        this.provider = provider;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}


