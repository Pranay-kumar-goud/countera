package com.countera.assessment;

import java.math.BigDecimal;

public record SaleItem(String sku, int qty, BigDecimal unitPrice) {
}
