package com.stockpulse.suggestion;

import com.stockpulse.product.StockSignalEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class StockSignalEventListener {

    private final StockSignalEvaluationService evaluationService;

    public StockSignalEventListener(StockSignalEvaluationService evaluationService) {
        this.evaluationService = evaluationService;
    }

    @Async("suggestionTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockSignal(StockSignalEvent event) {
        if (event.triggersEvaluation()) {
            evaluationService.evaluate(event);
        }
    }
}