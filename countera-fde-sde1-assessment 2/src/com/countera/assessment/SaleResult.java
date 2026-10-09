package com.countera.assessment;

public record SaleResult(
        boolean success,
        String code,
        String message,
        SaleReceipt sale) {

    public static SaleResult success(SaleReceipt sale) {
        return new SaleResult(true, "OK", "Sale accepted", sale);
    }

    public static SaleResult error(String code, String message) {
        return new SaleResult(false, code, message, null);
    }
}
