package com.countera.assessment;

public interface SaleRepository {
    SaveOutcome saveIfAbsent(NormalizedSaleRequest request, SaleReceipt candidateReceipt);

    int size();

    enum SaveStatus {
        CREATED,
        EXISTING_SAME_REQUEST,
        IDEMPOTENCY_CONFLICT
    }

    record SaveOutcome(SaveStatus status, SaleReceipt receipt) {
    }
}
