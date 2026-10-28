package com.stockpulse.product;

/**
 * Signal categories that the asynchronous evaluator may derive from a mutation snapshot.
 *
 * <p>The mutation event intentionally carries the snapshot rather than a precomputed demand
 * signal. Demand-spike detection needs peer-product data and therefore belongs to evaluation.</p>
 */
public enum StockSignalType {
    INVENTORY_LOW,
    DEMAND_SPIKE
}