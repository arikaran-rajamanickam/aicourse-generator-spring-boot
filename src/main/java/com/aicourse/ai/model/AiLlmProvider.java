package com.aicourse.ai.model;

import com.aicourse.ai.AiProviderType;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "ai_llm_providers")
public class AiLlmProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 30)
    private AiProviderType providerType;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "base_url")
    private String baseUrl;

    @Column(name = "key_cooldown_hours", nullable = false)
    private Integer keyCooldownHours = 24;

    @Column(name = "enabled", nullable = false)
    private Boolean enabled = true;

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

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public AiProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(AiProviderType providerType) {
        this.providerType = providerType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public Integer getKeyCooldownHours() {
        return keyCooldownHours;
    }

    public void setKeyCooldownHours(Integer keyCooldownHours) {
        this.keyCooldownHours = keyCooldownHours;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
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

