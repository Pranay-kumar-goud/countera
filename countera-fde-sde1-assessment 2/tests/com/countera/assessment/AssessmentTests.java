package com.countera.assessment;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public final class AssessmentTests {
    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        run("normal successful sale", AssessmentTests::normalSuccessfulSale);
        run("same request submitted twice", AssessmentTests::sameRequestSubmittedTwice);
        run("invalid payment total", AssessmentTests::invalidPaymentTotal);
        run("concurrent duplicate request", AssessmentTests::concurrentDuplicateRequest);
        run("same requestId with different payload is rejected", AssessmentTests::idempotencyConflict);
        run("sub-cent money is rejected", AssessmentTests::subCentMoneyIsRejected);

        System.out.printf("%nResult: %d passed, %d failed%n", passed, failed);
        if (failed > 0) {
            throw new AssertionError("One or more tests failed");
        }
    }

    private static void normalSuccessfulSale() {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);

        SaleResult result = service.process(sampleRequest("req-9001"));

        assertTrue(result.success(), "sale should succeed");
        assertEquals("OK", result.code(), "success code");
        assertEquals(new BigDecimal("12.25"), result.sale().total(), "computed total");
        assertEquals(1, repository.size(), "one sale should be stored");
    }

    private static void sameRequestSubmittedTwice() {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);
        SaleRequest request = sampleRequest("req-duplicate");

        SaleResult first = service.process(request);
        SaleResult second = service.process(request);

        assertTrue(first.success(), "first request should succeed");
        assertEquals(first, second, "replay must return the same successful result");
        assertEquals(1, repository.size(), "replay must not create another sale");
    }

    private static void invalidPaymentTotal() {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);
        SaleRequest request = new SaleRequest(
                "req-bad-total",
                "FREMONT-01",
                "L07",
                List.of(new SaleItem("MILK-1G", 2, new BigDecimal("4.50"))),
                new Payment("CARD", new BigDecimal("8.99")));

        SaleResult result = service.process(request);

        assertFalse(result.success(), "invalid payment should fail");
        assertEquals("VALIDATION_ERROR", result.code(), "validation error code");
        assertEquals(0, repository.size(), "invalid sale must not be stored");
    }

    private static void concurrentDuplicateRequest() throws Exception {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);
        SaleRequest request = sampleRequest("req-race");

        int workers = 24;
        ExecutorService pool = Executors.newFixedThreadPool(workers);
        CountDownLatch ready = new CountDownLatch(workers);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<SaleResult>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < workers; i++) {
                Callable<SaleResult> task = () -> {
                    ready.countDown();
                    start.await();
                    return service.process(request);
                };
                futures.add(pool.submit(task));
            }

            ready.await();
            start.countDown();

            SaleResult expected = futures.get(0).get();
            assertTrue(expected.success(), "concurrent request should succeed");
            for (Future<SaleResult> future : futures) {
                assertEquals(expected, future.get(), "all retries must return the same result");
            }
            assertEquals(1, repository.size(), "concurrent retries must persist one sale");
        } finally {
            pool.shutdownNow();
        }
    }

    private static void idempotencyConflict() {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);

        SaleResult first = service.process(sampleRequest("req-conflict"));
        SaleRequest changed = new SaleRequest(
                "req-conflict",
                "FREMONT-01",
                "L07",
                List.of(new SaleItem("MILK-1G", 1, new BigDecimal("4.50"))),
                new Payment("CARD", new BigDecimal("4.50")));
        SaleResult second = service.process(changed);

        assertTrue(first.success(), "first use of requestId should succeed");
        assertFalse(second.success(), "different payload with same requestId should fail");
        assertEquals("IDEMPOTENCY_CONFLICT", second.code(), "conflict code");
        assertEquals(1, repository.size(), "conflict must not overwrite the original sale");
    }

    private static void subCentMoneyIsRejected() {
        InMemorySaleRepository repository = new InMemorySaleRepository();
        SaleService service = new SaleService(repository);
        SaleRequest request = new SaleRequest(
                "req-sub-cent",
                "FREMONT-01",
                "L07",
                List.of(new SaleItem("SKU-1", 1, new BigDecimal("1.001"))),
                new Payment("CARD", new BigDecimal("1.001")));

        SaleResult result = service.process(request);

        assertFalse(result.success(), "sub-cent values should be rejected");
        assertEquals("VALIDATION_ERROR", result.code(), "validation error code");
    }

    private static SaleRequest sampleRequest(String requestId) {
        return new SaleRequest(
                requestId,
                "FREMONT-01",
                "L07",
                List.of(
                        new SaleItem("MILK-1G", 2, new BigDecimal("4.50")),
                        new SaleItem("BREAD-01", 1, new BigDecimal("3.25"))),
                new Payment("CARD", new BigDecimal("12.25")));
    }

    private static void run(String name, ThrowingRunnable test) throws Exception {
        try {
            test.run();
            passed++;
            System.out.println("PASS - " + name);
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL - " + name + ": " + t.getMessage());
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " | expected=" + expected + ", actual=" + actual);
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
