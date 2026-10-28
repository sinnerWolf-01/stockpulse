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

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "pricing_suggestions", uniqueConstraints = @UniqueConstraint(name = "uk_pricing_suggestion_active_key", columnNames = "active_key"))
public class PricingSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String productId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal recommendedPrice;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Direction direction;

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
    private BigDecimal confidence;

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

    protected PricingSuggestion() { }

    public PricingSuggestion(String productId, BigDecimal currentPrice, BigDecimal recommendedPrice,
                              Direction direction, TriggerReason triggerReason, SuggestionSource source,
                              BigDecimal confidence, String reasoning, String activeKey) {
        this.productId = requireText(productId, "productId");
        this.currentPrice = requireValue(currentPrice, "currentPrice");
        this.recommendedPrice = requireValue(recommendedPrice, "recommendedPrice");
        this.direction = requireValue(direction, "direction");
        this.triggerReason = requireValue(triggerReason, "triggerReason");
        this.source = requireValue(source, "source");
        this.confidence = requireValue(confidence, "confidence");
        this.reasoning = requireText(reasoning, "reasoning");
        this.activeKey = requireText(activeKey, "activeKey");
        this.status = SuggestionStatus.PENDING;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }

    public void resolve(SuggestionStatus resolution, String note) {
        if (resolution != SuggestionStatus.ACCEPTED && resolution != SuggestionStatus.REJECTED) {
            throw new IllegalArgumentException("Suggestion resolution must be ACCEPTED or REJECTED");
        }
        if (status != SuggestionStatus.PENDING) {
            throw new IllegalStateException("Only pending suggestions can be resolved");
        }
        status = resolution;
        activeKey = null;
        resolutionNote = note;
        resolvedAt = Instant.now();
        updatedAt = resolvedAt;
    }

    public Long getId() { return id; }
    public String getProductId() { return productId; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public BigDecimal getRecommendedPrice() { return recommendedPrice; }
    public Direction getDirection() { return direction; }
    public TriggerReason getTriggerReason() { return triggerReason; }
    public SuggestionSource getSource() { return source; }
    public SuggestionStatus getStatus() { return status; }
    public BigDecimal getConfidence() { return confidence; }
    public String getReasoning() { return reasoning; }
    public String getActiveKey() { return activeKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolutionNote() { return resolutionNote; }

    private static String requireText(String value, String name) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
        return value;
    }

    private static <T> T requireValue(T value, String name) {
        if (value == null) throw new IllegalArgumentException(name + " is required");
        return value;
    }
}