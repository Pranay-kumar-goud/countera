package com.countera.assessment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SaleService {
    private static final int MONEY_SCALE = 2;

    private final SaleRepository repository;

    public SaleService(SaleRepository repository) {
        this.repository = repository;
    }

    public SaleResult process(SaleRequest request) {
        Validation validation = validateAndNormalize(request);
        if (!validation.valid()) {
            return SaleResult.error("VALIDATION_ERROR", validation.error());
        }

        NormalizedSaleRequest normalized = validation.request();
        SaleReceipt candidateReceipt = new SaleReceipt(
                UUID.randomUUID().toString(),
                normalized.requestId(),
                normalized.storeId(),
                normalized.laneId(),
                normalized.total());

        SaleRepository.SaveOutcome outcome = repository.saveIfAbsent(normalized, candidateReceipt);
        return switch (outcome.status()) {
            case CREATED, EXISTING_SAME_REQUEST -> SaleResult.success(outcome.receipt());
            case IDEMPOTENCY_CONFLICT -> SaleResult.error(
                    "IDEMPOTENCY_CONFLICT",
                    "requestId was already used for a different sale payload");
        };
    }

    private Validation validateAndNormalize(SaleRequest request) {
        if (request == null) {
            return Validation.error("request is required");
        }
        if (isBlank(request.requestId()) || isBlank(request.storeId()) || isBlank(request.laneId())) {
            return Validation.error("requestId, storeId, and laneId must be non-empty");
        }
        if (request.items() == null || request.items().isEmpty()) {
            return Validation.error("items must be non-empty");
        }
        if (request.payment() == null || isBlank(request.payment().type()) || request.payment().amount() == null) {
            return Validation.error("payment type and amount are required");
        }

        List<SaleItem> normalizedItems = new ArrayList<>(request.items().size());
        BigDecimal total = BigDecimal.ZERO.setScale(MONEY_SCALE);

        for (SaleItem item : request.items()) {
            if (item == null || isBlank(item.sku())) {
                return Validation.error("each item must have a non-empty sku");
            }
            if (item.qty() <= 0) {
                return Validation.error("item qty must be greater than 0");
            }
            if (item.unitPrice() == null || item.unitPrice().signum() < 0) {
                return Validation.error("item unitPrice must be greater than or equal to 0");
            }

            BigDecimal unitPrice;
            try {
                unitPrice = normalizeMoney(item.unitPrice());
            } catch (ArithmeticException ex) {
                return Validation.error("money values must have at most 2 decimal places");
            }

            SaleItem normalizedItem = new SaleItem(item.sku().trim(), item.qty(), unitPrice);
            normalizedItems.add(normalizedItem);
            total = total.add(unitPrice.multiply(BigDecimal.valueOf(item.qty())));
        }

        BigDecimal paymentAmount;
        try {
            paymentAmount = normalizeMoney(request.payment().amount());
        } catch (ArithmeticException ex) {
            return Validation.error("money values must have at most 2 decimal places");
        }

        total = total.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
        if (paymentAmount.compareTo(total) != 0) {
            return Validation.error("payment amount must equal computed item total");
        }

        NormalizedSaleRequest normalized = new NormalizedSaleRequest(
                request.requestId().trim(),
                request.storeId().trim(),
                request.laneId().trim(),
                List.copyOf(normalizedItems),
                new Payment(request.payment().type().trim(), paymentAmount),
                total);
        return Validation.success(normalized);
    }

    private static BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Validation(boolean valid, NormalizedSaleRequest request, String error) {
        private static Validation success(NormalizedSaleRequest request) {
            return new Validation(true, request, null);
        }

        private static Validation error(String message) {
            return new Validation(false, null, message);
        }
    }
}
