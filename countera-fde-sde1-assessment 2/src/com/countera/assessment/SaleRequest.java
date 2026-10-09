package com.countera.assessment;

import java.util.List;

public record SaleRequest(
        String requestId,
        String storeId,
        String laneId,
        List<SaleItem> items,
        Payment payment) {
}
