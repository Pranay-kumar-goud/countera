package com.countera.assessment;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public final class InMemorySaleRepository implements SaleRepository {
    private final ConcurrentMap<String, StoredSale> salesByRequestId = new ConcurrentHashMap<>();

    @Override
    public SaveOutcome saveIfAbsent(NormalizedSaleRequest request, SaleReceipt candidateReceipt) {
        StoredSale candidate = new StoredSale(request, candidateReceipt);
        StoredSale existing = salesByRequestId.putIfAbsent(request.requestId(), candidate);

        if (existing == null) {
            return new SaveOutcome(SaveStatus.CREATED, candidateReceipt);
        }
        if (existing.request().equals(request)) {
            return new SaveOutcome(SaveStatus.EXISTING_SAME_REQUEST, existing.receipt());
        }
        return new SaveOutcome(SaveStatus.IDEMPOTENCY_CONFLICT, existing.receipt());
    }

    @Override
    public int size() {
        return salesByRequestId.size();
    }

    private record StoredSale(NormalizedSaleRequest request, SaleReceipt receipt) {
    }
}
