# Part 3 — Production Incident: Store Checkout Is Degrading

## First 5 minutes

1. Declare/acknowledge the incident, pause further releases, and assign one place for notes/communication.
2. Correlate the timing: compare latency/error/DB-connection graphs before and after the release 25 minutes ago; check whether all instances/stores are affected.
3. Inspect connection-pool metrics (active, idle, wait time, acquisition timeouts), PostgreSQL sessions/slow queries, and application logs/traces for the failing checkout path.
4. Review the release diff for connection leaks, longer transactions, new queries, retry loops, or changed pool settings.
5. Avoid speculative schema changes, increasing the connection pool blindly, killing DB sessions indiscriminately, or restarting every instance at once. Those actions can increase load or risk in-flight sales.

## Decision

I would roll back if the degradation began with the release, the affected code path changed in that release, and rollback is known to be data/schema compatible. I would also prefer rollback when the root cause is not yet proven but the release is the strongest evidence and rollback is the fastest reversible way to restore checkout health.

I would continue debugging the current release if the same symptoms clearly predate it, the issue is isolated to an unchanged downstream/database component, or rollback itself is unsafe because of an incompatible data migration. In either case I would make the decision from telemetry and change history, not from timing alone.

## Stabilize

- If rollback criteria are met, roll back progressively and watch checkout latency, error rate, and DB connection usage before expanding the rollback.
- Reduce non-essential database work and background jobs, and cap request/worker concurrency so checkout traffic can acquire connections. Do not increase the pool beyond database capacity.
- Keep sale processing durable and idempotent. Retry transient failures with bounded backoff; do not drop sale events or bypass uniqueness/inventory checks to make latency look better.

## Communicate

We are investigating elevated checkout latency and errors and have correlated the incident with very high database connection usage. A release went out shortly before the symptoms, so we are validating that change and preparing a safe rollback if the evidence continues to point there. Checkout correctness remains protected; we are not bypassing duplicate-sale or inventory safeguards. I do not have a reliable recovery ETA yet, and I will provide the next update after the rollback/diagnostic checkpoint.
