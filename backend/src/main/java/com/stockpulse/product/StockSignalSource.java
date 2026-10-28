package com.stockpulse.product;

public enum StockSignalSource {
    CUSTOMER_ORDER,
    MANUAL_STOCK_PATCH,
    PRODUCT_CREATED,
    REORDER_ACCEPTANCE;

    /**
     * Reorder acceptance changes inventory but must not recursively create another suggestion.
     */
    public boolean triggersEvaluation() {
        return this != REORDER_ACCEPTANCE;
    }
}