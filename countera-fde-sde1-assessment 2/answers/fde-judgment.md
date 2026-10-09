# Part 4 — FDE Judgment & Working Style

## 4.1 Ambiguous customer request

**Assumption:** “Receiving” means the customer's inbound inventory/receiving workflow, and the issue is important enough to affect tomorrow-morning operations.

1. **Acknowledge and establish impact.** Record store/site, affected users/devices, when it started, what “broken” means operationally, and the required morning deadline from the information already available.
2. **Check evidence before changing anything.** Review logs, traces, alerts, failed jobs/messages, integration status, and error rates for the receiving workflow around the reported time.
3. **Check recent change history.** Look for deployments, configuration changes, feature flags, credential/certificate changes, vendor/API incidents, or data/schema changes that could explain the failure.
4. **Try to reproduce with the closest safe conditions.** Use a test/staging flow or representative input, and compare the affected site's telemetry with a healthy site/customer to narrow the difference.
5. **Create the next-action plan despite the reporter being offline.** Add targeted logging if safe, document hypotheses and evidence, prepare specific questions for the reporter, notify the support/operations owner of current risk, and set a concrete next checkpoint. I would not ship a speculative fix merely to meet the morning deadline.

## 4.2 Pressure and ownership

I pause the planned feature and preserve the work in a clean local branch/commit so it can be resumed safely.  
I take ownership of the P0 until the tech lead is reachable, notify the incident/operations channel, and gather the minimum evidence needed to understand customer impact.  
I prioritize a reversible mitigation or rollback over a risky code change, while protecting sale, payment, and inventory correctness.  
I document timestamps, observations, actions, and results so another engineer can join without restarting the investigation.  
If I need a decision outside my authority, I escalate to the next available owner rather than waiting silently.  
Once stable, I hand off or close the incident with clear follow-ups, then return to the feature using the saved context and update its delivery expectation.

## 4.3 Disagreement

I would challenge the patch with the concrete failure mode, not with seniority: “Under a retry, two requests can both create a record, so we could duplicate a sale.” I would propose the fastest safe alternative, such as an atomic insert protected by a unique `request_id` constraint/idempotency key, and offer to pair on a focused test that reproduces the retry race. If the customer needs an immediate mitigation, I would prefer a reversible flag/rollback or temporary traffic control that preserves correctness. If we still disagree, I would ask for a quick second review from the available incident owner while continuing work on the safe option. The goal is to keep progress moving without accepting a known data-integrity risk.
