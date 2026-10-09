# Countera FDE / SDE I Engineering Assessment

A deliberately small Java solution for the checkout-event exercise, plus concise answers for the CS fundamentals, production incident, and FDE judgment sections.

## Runtime

- Java 17+
- No external libraries or frameworks

## Run

From the repository root:

```bash
./run_tests.sh
```

Equivalent commands if the script cannot be executed directly:

```bash
rm -rf out && mkdir -p out
javac --release 17 -Xlint:all -d out $(find src tests -name '*.java' | sort)
java -ea -cp out com.countera.assessment.AssessmentTests
```

Expected result: 6 tests pass.

## Repository layout

```text
README.md
run_tests.sh
src/                         # checkout implementation
  com/countera/assessment/
tests/                       # automated tests
  com/countera/assessment/
answers/
  fundamentals.md            # Part 2
  incident.md                # Part 3
  fde-judgment.md            # Part 4
```

## Assumptions / scope

- `requestId` is the caller-provided idempotency key for one logical completed sale.
- Replaying the same normalized payload with the same `requestId` returns the same successful receipt.
- Reusing a `requestId` for a different payload is treated as an idempotency conflict instead of silently accepting different data.
- USD-style monetary values are represented with `BigDecimal` at exactly two decimal places; sub-cent values are rejected rather than rounded silently.
- A successful sale is the persistence event modeled here. A real external payment side effect would need the same idempotency key and a transactional/outbox-style workflow.
- HTTP/framework setup, authentication, cloud deployment, and UI were intentionally excluded because the prompt prioritizes correctness and reasoning over framework ceremony.

## Part 1 design note (max 10 lines)

1. Computing the sale total is O(n) over the items; validation is also O(n).
2. In-memory `requestId` lookup uses `ConcurrentHashMap`, with expected O(1) lookup/insert.
3. `putIfAbsent` is atomic, so concurrent requests cannot persist two sales for one `requestId`.
4. A reused `requestId` with a different normalized payload returns `IDEMPOTENCY_CONFLICT`.
5. Money uses `BigDecimal` at scale 2; no binary floating-point arithmetic is used for totals.
6. In PostgreSQL, `request_id` would have a UNIQUE B-tree index and sale creation would run in a transaction.
7. With multiple app instances, the database constraint—not an in-process lock—would enforce uniqueness atomically.
8. For an external payment call, I would also pass `requestId` as its idempotency key and use an outbox/state machine for safe retries.

## Error behavior

The service returns structured error codes/messages for validation and idempotency conflicts. Internal exceptions/stack traces are not exposed as responses. In an HTTP adapter I would map validation failures to `400`, idempotency payload conflicts to `409`, and successful create/replay to a consistent successful response containing the stored receipt.

## Test coverage

The automated tests cover:

- normal successful sale
- exact replay of the same request
- invalid payment total
- concurrent submissions of the same `requestId`
- same `requestId` with a different payload
- sub-cent money rejection

## If I had more time

I would add a PostgreSQL repository using `INSERT ... ON CONFLICT`/a unique constraint, integration tests around transaction behavior, structured logging/metrics, and a small HTTP adapter. If charging were part of this service, I would explicitly model payment states and use the same idempotency key across the downstream payment boundary.

## AI assistance disclosure

I used ChatGPT to review the assessment requirements, brainstorm edge cases (especially idempotency/concurrency and incident handling), draft/refine portions of the Java implementation and tests, and improve the clarity of the written answers/README. I reviewed the resulting code and reasoning for consistency with the prompt. No proprietary code, credentials, customer data, or material from a current/past employer was used.
