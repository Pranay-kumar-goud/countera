package com.countera.assessment;

import java.math.BigDecimal;
import java.util.List;

record NormalizedSaleRequest(
        String requestId,
        String storeId,
        String laneId,
        List<SaleItem> items,
        Payment payment,
        BigDecimal total) {
}
