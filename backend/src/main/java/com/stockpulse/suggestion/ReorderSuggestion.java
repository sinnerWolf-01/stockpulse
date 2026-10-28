package com.stockpulse.suggestion;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "reorder_suggestions", uniqueConstraints = @UniqueConstraint(name = "uk_reorder_suggestion_active_key", columnNames = "active_key"))
public class ReorderSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String productId;

    @Column(nullable = false)
    private int recommendedQuantity;

    @Column(nullable = false)
    private int leadTimeDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TriggerReason triggerReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SuggestionSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private SuggestionStatus status;

    @Column(nullable = false, precision = 5, scale = 4)
    private java.math.BigDecimal confidence;

    @Column(nullable = false, length = 2000)
    private String reasoning;

    @Column(name = "active_key", unique = true, length = 128)
    private String activeKey;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant resolvedAt;

    @Column(length = 1000)
    private String resolutionNote;

    protected ReorderSuggestion() { }

    public ReorderSuggestion(String productId, int recommendedQuantity, int leadTimeDays,
                              TriggerReason triggerReason, SuggestionSource source,
                              java.math.BigDecimal confidence, String reasoning, String activeKey) {
        if (productId == null || productId.isBlank()) throw new IllegalArgumentException("productId is required");
        if (recommendedQuantity <= 0) throw new IllegalArgumentException("recommendedQuantity must be positive");
        if (leadTimeDays < 0) throw new IllegalArgumentException("leadTimeDays cannot be negative");
        if (triggerReason == null || source == null || confidence == null) throw new IllegalArgumentException("Suggestion metadata is required");
        if (reasoning == null || reasoning.isBlank()) throw new IllegalArgumentException("reasoning is required");
        this.productId = productId;
        this.recommendedQuantity = recommendedQuantity;
        this.leadTimeDays = leadTimeDays;
        this.triggerReason = triggerReason;
        this.source = source;
        this.confidence = confidence;
        this.reasoning = reasoning;
        if (activeKey == null || activeKey.isBlank()) throw new IllegalArgumentException("activeKey is required");
        this.activeKey = activeKey;
        this.status = SuggestionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void resolve(SuggestionStatus resolution, String note) {
        if (resolution != SuggestionStatus.ACCEPTED && resolution != SuggestionStatus.REJECTED) throw new IllegalArgumentException("Suggestion resolution must be ACCEPTED or REJECTED");
        if (status != SuggestionStatus.PENDING) throw new IllegalStateException("Only pending suggestions can be resolved");
        status = resolution;
        activeKey = null;
        resolutionNote = note;
        resolvedAt = Instant.now();
        updatedAt = resolvedAt;
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public int getRecommendedQuantity() { return recommendedQuantity; }
    public int getLeadTimeDays() { return leadTimeDays; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public SuggestionSource getSource() { return source; }
    public SuggestionStatus getStatus() { return status; }
    public java.math.BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public String getActiveKey() { return activeKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }
}