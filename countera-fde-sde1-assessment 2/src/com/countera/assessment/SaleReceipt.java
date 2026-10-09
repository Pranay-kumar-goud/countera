package com.countera.assessment;

import java.math.BigDecimal;

public record SaleReceipt(
        String saleId,
        String requestId,
        String storeId,
        String laneId,
        BigDecimal total) {
}
