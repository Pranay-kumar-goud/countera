# Part 2 — Computer Science Fundamentals

## 2.1 Hot SKU lookup

| Approach | Expected lookup | Practical trade-off |
|---|---:|---|
| Linear scan of 100,000 variants | O(n) | Simplest representation and no index-maintenance cost, but every checkout lookup may examine many records, so latency grows with catalog size. |
| Hash map keyed by barcode | O(1) average, O(n) worst case | Excellent for exact in-memory barcode lookup, but uses additional memory and must be kept synchronized with the source of truth; it is not useful for ordered/range queries. |
| Database B-tree index on barcode | O(log n) | Durable and works across application instances, but adds storage/write-maintenance cost and each lookup also pays database/network/connection-pool overhead. |

For the checkout hot path, I would use an in-process cache/hash map only when staleness rules are acceptable, while keeping the database's indexed barcode column as the durable source of truth.

## 2.2 Queue behavior

The producer exceeds the worker by **400 events/sec** (1,200 - 800). Over 5 minutes (300 seconds), the queue grows by approximately **120,000 events**, assuming no scaling, rejection, or other bottleneck. That also increases end-to-end latency even if the queue itself remains healthy.

Two protections I would add:

1. **Controlled consumer scaling with downstream limits.** Scale workers based on queue depth/oldest-message age, but cap concurrency so the database and dependent services are not overwhelmed.
2. **Backpressure and overload protection.** Keep the queue durable, bound in-flight work, apply rate limits/admission control where appropriate, and alert on queue depth/age. Failed messages should retry with limits and move to a DLQ rather than spin forever.

The key is not to "fix" backlog by creating a new failure mode such as exhausting PostgreSQL connections.

## 2.3 Concurrency

The correctness problem is **overselling / lost-update behavior**: both workers observe quantity 1, both believe they can sell it, and inventory can become inconsistent.

A database-level protection is an atomic conditional update, for example:

```sql
UPDATE inventory
SET quantity = quantity - 1
WHERE sku = ? AND quantity >= 1;
```

The sale proceeds only when exactly one row was updated. This keeps the check and decrement atomic in the database. A row lock (`SELECT ... FOR UPDATE`) or serializable transaction is another valid approach.

**Trade-off:** hot SKUs can create lock/contention pressure, retries, and higher latency. The application therefore needs short transactions, retry handling, and metrics around lock/transaction failures.
